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
            .lowercase()
            .replace("@", "_at_")
            .replace(Regex("[.#$\\[\\]]"), "_")

    private fun issuesRef(studentId: String) =
        database.reference
            .child("students")
            .child(sanitizeKey(studentId))
            .child("issues")

    suspend fun backupIssue(issue: IssueEntity) = withContext(Dispatchers.IO) {
        val cleanRegNo = issue.studentEmail.trim()
        if (cleanRegNo.isBlank()) return@withContext

        val effectiveSyncKey = issue.syncKey.trim().ifBlank { UUID.randomUUID().toString() }
        val record = FirebaseIssueRecord(
            syncKey = effectiveSyncKey,
            studentEmail = cleanRegNo,
            title = issue.title,
            description = issue.description,
            category = issue.category,
            status = issue.status,
            timeAgo = issue.timeAgo,
            createdAt = issue.createdAt
        )
        issuesRef(cleanRegNo)
            .child(effectiveSyncKey)
            .setValue(record)
            .await()
    }

    suspend fun deleteIssueFromFirebase(issue: IssueEntity) = withContext(Dispatchers.IO) {
        val cleanRegNo = issue.studentEmail.trim()
        val cleanSyncKey = issue.syncKey.trim()
        if (cleanRegNo.isBlank() || cleanSyncKey.isBlank()) return@withContext

        issuesRef(cleanRegNo)
            .child(cleanSyncKey)
            .removeValue()
            .await()
    }

    suspend fun backupAllIssuesToFirebase(email: String) = withContext(Dispatchers.IO) {
        val cleanRegNo = email.trim()
        if (cleanRegNo.isBlank()) return@withContext

        val localIssues = dao.getIssuesOnceForStudent(cleanRegNo)
        localIssues.forEach { issue ->
            backupIssue(issue)
        }
    }

    suspend fun restoreIssuesFromFirebase(email: String) = withContext(Dispatchers.IO) {
        val cleanRegNo = email.trim()
        if (cleanRegNo.isBlank()) return@withContext

        val snapshot = issuesRef(cleanRegNo).get().await()
        for (child in snapshot.children) {
            val remote = child.getValue(FirebaseIssueRecord::class.java) ?: continue
            val effectiveSyncKey = remote.syncKey.trim().ifBlank { child.key.orEmpty().trim() }
            if (effectiveSyncKey.isBlank() || remote.title.isBlank()) continue

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
                title = remote.title,
                description = remote.description,
                category = remote.category.ifBlank { "MAINTENANCE" },
                status = remote.status.ifBlank { "OPEN" },
                timeAgo = remote.timeAgo.ifBlank { "Restored" },
                createdAt = resolvedCreatedAt
            )
            dao.insertIssue(entityToSave)
        }
    }
}