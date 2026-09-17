package com.spendly.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.usecase.SignInUseCase
import com.spendly.app.domain.usecase.SignInWithGoogleUseCase
import com.spendly.app.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) = launchAuthAction {
        signInUseCase(email, password)
    }

    fun signUp(email: String, password: String) = launchAuthAction {
        signUpUseCase(email, password)
    }

    fun signInWithGoogle(idToken: String) = launchAuthAction {
        signInWithGoogleUseCase(idToken)
    }

    private fun launchAuthAction(action: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = action()) {
                is AppResult.Success -> _uiState.value = AuthUiState(isAuthenticated = true)
                is AppResult.Error -> _uiState.value = AuthUiState(errorMessage = result.message)
                AppResult.Loading -> Unit
            }
        }
    }
}
