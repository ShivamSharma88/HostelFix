package com.example.hostelfix.data

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
    private fun sanitizeEmailKey(email: String): String =
        email.trim().lowercase().replace(".", "_").replace("@", "_at_")

    private fun issuesRef(email: String) =
        database.reference
            .child("students")
            .child(sanitizeEmailKey(email))
            .child("issues")

    suspend fun backupIssue(issue: IssueEntity) = withContext(Dispatchers.IO) {
        val record = FirebaseIssueRecord(
            syncKey = issue.syncKey,
            studentEmail = issue.studentEmail,
            title = issue.title,
            description = issue.description,
            category = issue.category,
            status = issue.status,
            timeAgo = issue.timeAgo,
            createdAt = issue.createdAt
        )
        issuesRef(issue.studentEmail)
            .child(issue.syncKey)
            .setValue(record)
            .await()
    }

    suspend fun deleteIssueFromFirebase(issue: IssueEntity) = withContext(Dispatchers.IO) {
        issuesRef(issue.studentEmail)
            .child(issue.syncKey)
            .removeValue()
            .await()
    }

    suspend fun backupAllIssuesToFirebase(email: String) = withContext(Dispatchers.IO) {
        val localIssues = dao.getIssuesOnceForStudent(email)
        localIssues.forEach { issue ->
            backupIssue(issue)
        }
    }

    suspend fun restoreIssuesFromFirebase(email: String) = withContext(Dispatchers.IO) {
        val snapshot = issuesRef(email).get().await()
        for (child in snapshot.children) {
            val remote = child.getValue(FirebaseIssueRecord::class.java) ?: continue
            if (remote.syncKey.isBlank() || remote.title.isBlank()) continue

            val existing = dao.getIssueBySyncKey(email, remote.syncKey)
            val resolvedCreatedAt = when {
                remote.createdAt > 0L -> remote.createdAt
                existing != null && existing.createdAt > 0L -> existing.createdAt
                else -> System.currentTimeMillis()
            }
            val entityToSave = IssueEntity(
                id = existing?.id ?: 0,
                syncKey = remote.syncKey,
                studentEmail = email,
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