package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.User
import com.spendly.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAuthRepository(
    private val signInResult: AppResult<User>
) : AuthRepository {
    override val currentUser = MutableStateFlow<User?>(null)

    override suspend fun signUpWithEmail(email: String, password: String) = signInResult
    override suspend fun signInWithEmail(email: String, password: String) = signInResult
    override suspend fun signInWithGoogleIdToken(idToken: String) = signInResult
    override suspend fun signOut(): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun getCurrentUser(): User? = null
}

class SignInUseCaseTest {

    @Test
    fun `sign in succeeds returns the signed in user`() = runBlocking {
        val expectedUser = User(id = "user-1", email = "test@example.com")
        val useCase = SignInUseCase(FakeAuthRepository(AppResult.Success(expectedUser)))

        val result = useCase("test@example.com", "password123")

        assertTrue(result is AppResult.Success)
        assertEquals(expectedUser, (result as AppResult.Success).data)
    }

    @Test
    fun `sign in failure returns an error result`() = runBlocking {
        val useCase = SignInUseCase(FakeAuthRepository(AppResult.Error("Invalid credentials")))

        val result = useCase("test@example.com", "wrong-password")

        assertTrue(result is AppResult.Error)
        assertEquals("Invalid credentials", (result as AppResult.Error).message)
    }
}
