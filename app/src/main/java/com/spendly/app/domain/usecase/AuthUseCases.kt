package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.User
import com.spendly.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignUpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<User> =
        repository.signUpWithEmail(email, password)
}

class SignInUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<User> =
        repository.signInWithEmail(email, password)
}

class SignInWithGoogleUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): AppResult<User> =
        repository.signInWithGoogleIdToken(idToken)
}

class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.signOut()
}

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    val currentUser: Flow<User?> = repository.currentUser
    suspend operator fun invoke(): User? = repository.getCurrentUser()
}
