package com.example.hostelfix.data

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FirebaseUserProfile(
    val username: String = "",
    val name: String = "",
    val hostelNumber: String = "",
    val roomNumber: String = "",
    val phoneNumber: String = "",
    val department: String = "",
    val linkedGoogleEmail: String? = null,
    val photoUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

class FirebaseProfileSync(
    private val dao: HostelDao,
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://hostelfix-app-74692-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
) {
    private fun sanitizeEmailKey(email: String): String =
        email.trim().lowercase().replace(".", "_").replace("@", "_at_")

    private fun profileRef(usernameOrEmail: String) =
        database.reference
            .child("students")
            .child(sanitizeEmailKey(usernameOrEmail))
            .child("profile")

    suspend fun backupProfile(user: UserEntity) = withContext(Dispatchers.IO) {
        val profile = FirebaseUserProfile(
            username = user.username.trim().lowercase(),
            name = user.name.trim(),
            hostelNumber = user.hostelNumber.trim(),
            roomNumber = user.roomNumber.trim(),
            phoneNumber = user.phoneNumber.trim(),
            department = user.department.trim(),
            linkedGoogleEmail = user.linkedGoogleEmail?.trim()?.lowercase()?.ifBlank { null },
            photoUrl = user.photoUrl,
            updatedAt = System.currentTimeMillis()
        )
        profileRef(user.username).setValue(profile).await()
    }

    suspend fun restoreProfile(usernameOrEmail: String) = withContext(Dispatchers.IO) {
        val cleanKey = usernameOrEmail.trim().lowercase()
        val remote = profileRef(cleanKey).get().await()
            .getValue(FirebaseUserProfile::class.java) ?: return@withContext

        val existing = dao.getUserByUsername(cleanKey) ?: dao.getUserByUsername(usernameOrEmail.trim())
        if (existing != null) {
            dao.updateUserDetails(
                username = existing.username,
                name = remote.name.trim().ifBlank { existing.name },
                hostelNumber = remote.hostelNumber.trim().ifBlank { existing.hostelNumber },
                roomNumber = remote.roomNumber.trim().ifBlank { existing.roomNumber },
                phoneNumber = remote.phoneNumber.trim().ifBlank { existing.phoneNumber },
                department = remote.department.trim().ifBlank { existing.department },
                linkedGoogleEmail = remote.linkedGoogleEmail?.trim()?.lowercase()?.ifBlank { null }
                    ?: existing.linkedGoogleEmail
            )
        }
    }
}