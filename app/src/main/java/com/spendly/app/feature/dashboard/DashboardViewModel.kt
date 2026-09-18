package com.spendly.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.engine.DashboardWarningLevel
import com.spendly.app.domain.engine.UpcomingCommitments
import com.spendly.app.domain.engine.calculateBudgetStatuses
import com.spendly.app.domain.engine.calculateDashboardWarningLevel
import com.spendly.app.domain.engine.calculateSafeToSpend
import com.spendly.app.domain.engine.calculateUpcomingCommitments
import com.spendly.app.domain.model.Profile
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.affectsBalance
import com.spendly.app.domain.usecase.GetActiveSubscriptionsUseCase
import com.spendly.app.domain.usecase.GetBudgetsUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetGoalsUseCase
import com.spendly.app.domain.usecase.GetProfileUseCase
import com.spendly.app.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

private const val UPCOMING_WINDOW_DAYS = 30

data class DashboardUiState(
    val profile: Profile? = null,
    val balance: Double? = null,
    val safeToSpend: Double? = null,
    val warningLevel: DashboardWarningLevel = DashboardWarningLevel.NORMAL,
    // Forward-looking forecast only - never folded into safeToSpend/balance above.
    val upcomingCommitments: UpcomingCommitments = UpcomingCommitments(0.0, 0.0)
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getCurrentUserUseCase: GetCurrentUserUseCase,
    getProfileUseCase: GetProfileUseCase,
    getTransactionsUseCase: GetTransactionsUseCase,
    getBudgetsUseCase: GetBudgetsUseCase,
    getActiveSubscriptionsUseCase: GetActiveSubscriptionsUseCase,
    getGoalsUseCase: GetGoalsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        getCurrentUserUseCase.currentUser
            .flatMapLatest { user ->
                if (user == null) return@flatMapLatest MutableStateFlow(DashboardUiState())
                combine(
                    getProfileUseCase.observe(user.id),
                    getTransactionsUseCase(user.id),
                    getBudgetsUseCase(user.id),
                    getActiveSubscriptionsUseCase(user.id),
                    getGoalsUseCase(user.id)
                ) { profile, transactions, budgets, subscriptions, goals ->
                    val net = transactions.filter { it.affectsBalance }.sumOf {
                        if (it.type == TransactionType.INCOME) it.amount else -it.amount
                    }
                    val balance = profile?.currentBalance?.plus(net)
                    val budgetStatuses = calculateBudgetStatuses(budgets, transactions)
                    val safeToSpend = balance?.let { calculateSafeToSpend(it, budgetStatuses) }
                    val warningLevel = balance?.let { calculateDashboardWarningLevel(it, budgetStatuses) }
                        ?: DashboardWarningLevel.NORMAL
                    val upcomingCommitments = calculateUpcomingCommitments(
                        subscriptions = subscriptions,
                        goals = goals,
                        now = System.currentTimeMillis(),
                        windowDays = UPCOMING_WINDOW_DAYS
                    )
                    DashboardUiState(
                        profile = profile,
                        balance = balance,
                        safeToSpend = safeToSpend,
                        warningLevel = warningLevel,
                        upcomingCommitments = upcomingCommitments
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }
}
