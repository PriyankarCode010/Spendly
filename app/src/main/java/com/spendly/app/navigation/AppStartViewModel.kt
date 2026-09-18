package com.spendly.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.usecase.EnsureDefaultCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StartDestination {
    data object Loading : StartDestination()
    data object SignedOut : StartDestination()
    data object NeedsFinancialSetup : StartDestination()
    data object Ready : StartDestination()
}

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val ensureDefaultCategoriesUseCase: EnsureDefaultCategoriesUseCase
) : ViewModel() {

    private val _startDestination = MutableStateFlow<StartDestination>(StartDestination.Loading)
    val startDestination: StateFlow<StartDestination> = _startDestination.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _startDestination.value = StartDestination.Loading
            val user = getCurrentUserUseCase()
            if (user != null) ensureDefaultCategoriesUseCase(user.id)
            _startDestination.value = when {
                user == null -> StartDestination.SignedOut
                getProfileUseCase(user.id) == null -> StartDestination.NeedsFinancialSetup
                else -> StartDestination.Ready
            }
        }
    }
}
