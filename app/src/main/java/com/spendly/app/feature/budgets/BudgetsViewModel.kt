package com.spendly.app.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.engine.BudgetStatus
import com.spendly.app.domain.engine.DashboardWarningLevel
import com.spendly.app.domain.engine.calculateBudgetStatuses
import com.spendly.app.domain.engine.calculateDashboardWarningLevel
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.BudgetPeriod
import com.spendly.app.domain.model.BudgetPriority
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.affectsBalance
import com.spendly.app.domain.usecase.AddBudgetUseCase
import com.spendly.app.domain.usecase.DeleteBudgetUseCase
import com.spendly.app.domain.usecase.GetBudgetsUseCase
import com.spendly.app.domain.usecase.GetCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetProfileUseCase
import com.spendly.app.domain.usecase.GetTransactionsUseCase
import com.spendly.app.domain.usecase.UpdateBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BudgetsUiState(
    val statuses: List<BudgetStatus> = emptyList(),
    val dashboardWarningLevel: DashboardWarningLevel = DashboardWarningLevel.NORMAL
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val addBudgetUseCase: AddBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val categories: StateFlow<List<Category>> = getCurrentUserUseCase.currentUser
        .flatMapLatest { user -> user?.let { getCategoriesUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user ->
                if (user == null) return@flatMapLatest MutableStateFlow(BudgetsUiState())
                combine(
                    getBudgetsUseCase(user.id),
                    getTransactionsUseCase(user.id),
                    getProfileUseCase.observe(user.id)
                ) { budgets, transactions, profile ->
                    val statuses = calculateBudgetStatuses(budgets, transactions)
                    val net = transactions.filter { it.affectsBalance }.sumOf {
                        if (it.type == TransactionType.INCOME) it.amount else -it.amount
                    }
                    val balance = (profile?.currentBalance ?: 0.0) + net
                    BudgetsUiState(
                        statuses = statuses,
                        dashboardWarningLevel = calculateDashboardWarningLevel(balance, statuses)
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addBudget(categoryId: String?, amount: Double, priority: BudgetPriority, period: BudgetPeriod) {
        val userId = currentUserId ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            reportIfError(
                addBudgetUseCase(
                    Budget(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        categoryId = categoryId,
                        amount = amount,
                        priority = priority,
                        period = period,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            )
        }
    }

    fun updateBudget(original: Budget, categoryId: String?, amount: Double, priority: BudgetPriority, period: BudgetPeriod) {
        viewModelScope.launch {
            reportIfError(
                updateBudgetUseCase(
                    original.copy(
                        categoryId = categoryId,
                        amount = amount,
                        priority = priority,
                        period = period,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            reportIfError(deleteBudgetUseCase(id))
        }
    }

    private fun reportIfError(result: AppResult<Unit>) {
        if (result is AppResult.Error) {
            _errorMessage.value = result.message
        }
    }
}
