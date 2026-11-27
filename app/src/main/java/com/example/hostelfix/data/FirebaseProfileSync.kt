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
    private fun sanitizeKey(raw: String): String =
        raw.trim()
            .lowercase()
            .replace("@", "_at_")
            .replace(Regex("[.#$\\[\\]]"), "_")

    private fun profileRef(regNo: String) =
        database.reference
            .child("students")
            .child(sanitizeKey(regNo))
            .child("profile")

    private fun googleLinkRef(email: String) =
        database.reference
            .child("google_links")
            .child(sanitizeKey(email))

    suspend fun findRegNoForGoogleEmail(email: String): String? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return@withContext null

        dao.getUserByLinkedGoogleEmail(cleanEmail)?.username?.let { return@withContext it }

        val mappedRegNo = googleLinkRef(cleanEmail).get().await()
            .getValue(String::class.java)?.trim()
        if (!mappedRegNo.isNullOrBlank()) {
            return@withContext mappedRegNo
        }

        val studentsSnapshot = database.reference.child("students").get().await()
        for (studentSnap in studentsSnapshot.children) {
            val profile = studentSnap.child("profile").getValue(FirebaseUserProfile::class.java)
            val profileEmail = profile?.linkedGoogleEmail?.trim()?.lowercase()
            val regNo = profile?.username?.trim()?.ifBlank { studentSnap.key.orEmpty() }
            if (profileEmail == cleanEmail && !regNo.isNullOrBlank()) {
                googleLinkRef(cleanEmail).setValue(regNo).await()
                return@withContext regNo
            }
        }

        null
    }

    suspend fun backupProfile(user: UserEntity) = withContext(Dispatchers.IO) {
        val cleanRegNo = user.username.trim()
        val cleanEmail = user.linkedGoogleEmail?.trim()?.lowercase()?.ifBlank { null }

        if (cleanEmail != null) {
            val existingOwner = googleLinkRef(cleanEmail).get().await()
                .getValue(String::class.java)?.trim()
            if (!existingOwner.isNullOrBlank() && !existingOwner.equals(cleanRegNo, ignoreCase = true)) {
                throw IllegalStateException("This Google account is already linked to registration number $existingOwner.")
            }
            googleLinkRef(cleanEmail).setValue(cleanRegNo).await()
        }

        val profile = FirebaseUserProfile(
            username = cleanRegNo,
            name = user.name.trim(),
            hostelNumber = user.hostelNumber.trim(),
            roomNumber = user.roomNumber.trim(),
            phoneNumber = user.phoneNumber.trim(),
            department = user.department.trim(),
            linkedGoogleEmail = cleanEmail,
            photoUrl = user.photoUrl,
            updatedAt = System.currentTimeMillis()
        )
        profileRef(cleanRegNo).setValue(profile).await()
    }

    suspend fun restoreProfile(regNo: String) = withContext(Dispatchers.IO) {
        val cleanRegNo = regNo.trim()
        if (cleanRegNo.isBlank()) return@withContext

        val remote = profileRef(cleanRegNo).get().await()
            .getValue(FirebaseUserProfile::class.java) ?: return@withContext

        val remoteUsername = remote.username.trim()
        val existing = dao.getUserByUsername(cleanRegNo)
            ?: if (remoteUsername.isNotBlank()) dao.getUserByUsername(remoteUsername) else null

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