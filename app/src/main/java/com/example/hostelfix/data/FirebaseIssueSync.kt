package com.example.hostelfix.data

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class FirebaseIssueRecord(
    val syncKey: String = "",
    val studentEmail: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "MAINTENANCE",
    val status: String = "OPEN",
    val timeAgo: String = "Just now",
    val createdAt: Long = System.currentTimeMillis()
)

class FirebaseIssueSync(
    private val dao: HostelDao,
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://hostelfix-app-74692-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
) {
    private fun sanitizeKey(raw: String): String =
        raw.trim()
            .replace("@", "_at_")
            .replace(Regex("[.#$\\[\\]/]"), "_")

    private fun normalizeCategory(raw: String): String =
        when (raw.trim().uppercase()) {
            "ACADEMICAL" -> "ACADEMICAL"
            else -> "MAINTENANCE"
        }

    private fun normalizeStatus(raw: String): String =
        when (raw.trim().uppercase()) {
            "IN_PROGRESS" -> "IN_PROGRESS"
            "RESOLVED" -> "RESOLVED"
            else -> "OPEN"
        }

    private fun issuesRef(studentId: String) =
        database.reference
            .child("students")
            .child(sanitizeKey(studentId))
            .child("issues")

    suspend fun backupIssue(issue: IssueEntity) = withContext(Dispatchers.IO) {
        val cleanRegNo = issue.studentEmail.trim()
        val cleanTitle = issue.title.trim()
        if (!RegistrationNumberHelper.isValid(cleanRegNo) || cleanTitle.isBlank()) return@withContext

        val rawKey = issue.syncKey.trim().ifBlank { UUID.randomUUID().toString() }
        val effectiveSyncKey = sanitizeKey(rawKey).ifBlank { UUID.randomUUID().toString() }
        val normalizedCategory = normalizeCategory(issue.category)
        val normalizedStatus = normalizeStatus(issue.status)
        val resolvedCreatedAt = if (issue.createdAt > 0L) issue.createdAt else System.currentTimeMillis()
        val resolvedTimeAgo = issue.timeAgo.trim().ifBlank { "Just now" }

        val updatedIssue = issue.copy(
            syncKey = effectiveSyncKey,
            studentEmail = cleanRegNo,
            title = cleanTitle,
            description = issue.description.trim(),
            category = normalizedCategory,
            status = normalizedStatus,
            timeAgo = resolvedTimeAgo,
            createdAt = resolvedCreatedAt
        )
        if (updatedIssue != issue) {
            dao.insertIssue(updatedIssue)
        }

        val record = FirebaseIssueRecord(
            syncKey = effectiveSyncKey,
            studentEmail = cleanRegNo,
            title = cleanTitle,
            description = updatedIssue.description,
            category = normalizedCategory,
            status = normalizedStatus,
            timeAgo = resolvedTimeAgo,
            createdAt = resolvedCreatedAt
        )
        issuesRef(cleanRegNo)
            .child(effectiveSyncKey)
            .setValue(record)
            .await()
    }

    suspend fun deleteIssueFromFirebase(issue: IssueEntity) = withContext(Dispatchers.IO) {
        val cleanRegNo = issue.studentEmail.trim()
        val cleanSyncKey = sanitizeKey(issue.syncKey)
        if (!RegistrationNumberHelper.isValid(cleanRegNo) || cleanSyncKey.isBlank()) return@withContext

        issuesRef(cleanRegNo)
            .child(cleanSyncKey)
            .removeValue()
            .await()
    }

    suspend fun backupAllIssuesToFirebase(email: String) = withContext(Dispatchers.IO) {
        val cleanRegNo = email.trim()
        if (!RegistrationNumberHelper.isValid(cleanRegNo)) return@withContext

        val localIssues = dao.getIssuesOnceForStudent(cleanRegNo)
        localIssues.forEach { issue ->
            backupIssue(issue)
        }
    }

    suspend fun restoreIssuesFromFirebase(email: String) = withContext(Dispatchers.IO) {
        val cleanRegNo = email.trim()
        if (!RegistrationNumberHelper.isValid(cleanRegNo)) return@withContext

        val snapshot = issuesRef(cleanRegNo).get().await()
        for (child in snapshot.children) {
            val remote = child.getValue(FirebaseIssueRecord::class.java) ?: continue
            val rawKey = remote.syncKey.trim().ifBlank { child.key.orEmpty().trim() }
            val effectiveSyncKey = sanitizeKey(rawKey)
            val cleanTitle = remote.title.trim()
            if (effectiveSyncKey.isBlank() || cleanTitle.isBlank()) continue

            val existing = dao.getIssueBySyncKey(cleanRegNo, effectiveSyncKey)
            val resolvedCreatedAt = when {
                remote.createdAt > 0L -> remote.createdAt
                existing != null && existing.createdAt > 0L -> existing.createdAt
                else -> System.currentTimeMillis()
            }
            val entityToSave = IssueEntity(
                id = existing?.id ?: 0,
                syncKey = effectiveSyncKey,
                studentEmail = remote.studentEmail.trim().ifBlank { cleanRegNo },
                title = cleanTitle,
                description = remote.description.trim(),
                category = normalizeCategory(remote.category),
                status = normalizeStatus(remote.status),
                timeAgo = remote.timeAgo.trim().ifBlank { "Restored" },
                createdAt = resolvedCreatedAt
            )
            dao.insertIssue(entityToSave)
        }
    }
}