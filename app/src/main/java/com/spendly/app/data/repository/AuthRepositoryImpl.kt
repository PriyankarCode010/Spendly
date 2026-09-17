package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.remote.SupabaseAuthDataSource
import com.spendly.app.data.remote.mapToUserId
import com.spendly.app.domain.model.User
import com.spendly.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authDataSource: SupabaseAuthDataSource
) : AuthRepository {

    override val currentUser: Flow<User?> = authDataSource.sessionStatus.mapToUserId().map { userId ->
        userId?.let { User(id = it, email = authDataSource.currentUserEmail) }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AppResult<User> = runCatching {
        authDataSource.signUpWithEmail(email, password)
        requireNotNull(getCurrentUser()) { "Sign up succeeded but no session was returned" }
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(it.message ?: "Sign up failed", it) }
    )

    override suspend fun signInWithEmail(email: String, password: String): AppResult<User> = runCatching {
        authDataSource.signInWithEmail(email, password)
        requireNotNull(getCurrentUser()) { "Sign in succeeded but no session was returned" }
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(it.message ?: "Sign in failed", it) }
    )

    override suspend fun signInWithGoogleIdToken(idToken: String): AppResult<User> = runCatching {
        authDataSource.signInWithGoogleIdToken(idToken)
        requireNotNull(getCurrentUser()) { "Google sign in succeeded but no session was returned" }
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(it.message ?: "Google sign in failed", it) }
    )

    override suspend fun signOut(): AppResult<Unit> = runCatching {
        authDataSource.signOut()
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Sign out failed", it) }
    )

    override suspend fun getCurrentUser(): User? =
        authDataSource.currentUserId?.let { User(id = it, email = authDataSource.currentUserEmail) }
}
