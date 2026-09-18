package com.spendly.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.spendly.app.BuildConfig

/**
 * Wraps Credential Manager's Google ID flow. Must be called with an Activity
 * context (LocalContext.current from a composable) so the account picker UI
 * can be shown - an application-scoped context will fail here.
 */
class GoogleAuthClient(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun requestIdToken(): String {
        check(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            "GOOGLE_WEB_CLIENT_ID is not set in local.properties"
        }

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = try {
            credentialManager.getCredential(context, request)
        } catch (e: GetCredentialException) {
            throw IllegalStateException(
                "Google sign-in failed (${e.type}). If this mentions the developer console, " +
                    "the app's package name + SHA-1 aren't registered as an Android OAuth client " +
                    "in Google Cloud Console yet.",
                e
            )
        }
        return GoogleIdTokenCredential.createFrom(response.credential.data).idToken
    }
}
