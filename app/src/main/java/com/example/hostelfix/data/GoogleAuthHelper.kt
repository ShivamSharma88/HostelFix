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
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = webClientId
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
                Result.success(
                    PickedGoogleAccount(
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
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