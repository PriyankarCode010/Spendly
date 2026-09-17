package com.spendly.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    val sessionStatus: Flow<SessionStatus> = client.auth.sessionStatus

    val currentUserId: String?
        get() = client.auth.currentUserOrNull()?.id

    val currentUserEmail: String?
        get() = client.auth.currentUserOrNull()?.email

    suspend fun signUpWithEmail(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String) {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}

fun Flow<SessionStatus>.mapToUserId(): Flow<String?> = map { status ->
    (status as? SessionStatus.Authenticated)?.session?.user?.id
}
