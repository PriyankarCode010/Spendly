package com.spendly.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Profile
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.SaveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetupUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val saveProfileUseCase: SaveProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    fun saveFinancialSetup(currency: String, monthlyIncome: Double, currentBalance: Double) {
        viewModelScope.launch {
            _uiState.value = ProfileSetupUiState(isLoading = true)
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.value = ProfileSetupUiState(errorMessage = "You must be signed in")
                return@launch
            }

            val profile = Profile(
                userId = user.id,
                currency = currency,
                monthlyIncome = monthlyIncome,
                currentBalance = currentBalance,
                updatedAt = System.currentTimeMillis()
            )

            when (val result = saveProfileUseCase(profile)) {
                is AppResult.Success -> _uiState.value = ProfileSetupUiState(isSaved = true)
                is AppResult.Error -> _uiState.value = ProfileSetupUiState(errorMessage = result.message)
                AppResult.Loading -> Unit
            }
        }
    }
}
