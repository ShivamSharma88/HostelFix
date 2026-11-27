package com.example.hostelfix.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class PickedGoogleAccount(
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)

object GoogleAuthHelper {
    suspend fun pickGoogleAccount(
        context: Context,
        webClientId: String
    ): Result<PickedGoogleAccount> {
        val cleanClientId = webClientId.trim()
        if (cleanClientId.isBlank()) {
            return Result.failure(IllegalArgumentException("Google webClientId cannot be blank."))
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = cleanClientId
            ).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id.trim()
                if (email.isBlank()) {
                    return Result.failure(
                        IllegalStateException("Google account did not return an email address.")
                    )
                }

                Result.success(
                    PickedGoogleAccount(
                        email = email,
                        displayName = googleIdTokenCredential.displayName?.trim()?.ifBlank { null },
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString()?.ifBlank { null }
                    )
                )
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (_: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Google sign-in cancelled."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}