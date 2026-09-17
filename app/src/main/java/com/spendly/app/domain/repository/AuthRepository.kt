package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun signUpWithEmail(email: String, password: String): AppResult<User>
    suspend fun signInWithEmail(email: String, password: String): AppResult<User>
    suspend fun signInWithGoogleIdToken(idToken: String): AppResult<User>
    suspend fun signOut(): AppResult<Unit>
    suspend fun getCurrentUser(): User?
}
