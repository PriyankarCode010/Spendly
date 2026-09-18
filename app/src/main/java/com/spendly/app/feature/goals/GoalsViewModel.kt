package com.spendly.app.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.engine.calculateBudgetStatuses
import com.spendly.app.domain.engine.calculateSafeToSpend
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.affectsBalance
import com.spendly.app.domain.usecase.AddGoalUseCase
import com.spendly.app.domain.usecase.DeleteGoalUseCase
import com.spendly.app.domain.usecase.GetBudgetsUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetGoalsUseCase
import com.spendly.app.domain.usecase.GetProfileUseCase
import com.spendly.app.domain.usecase.GetTransactionsUseCase
import com.spendly.app.domain.usecase.UpdateGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    // Advisory only - never written back, never affects a goal's stored data.
    val currentSafeToSpend: Double? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val addGoalUseCase: AddGoalUseCase,
    private val updateGoalUseCase: UpdateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user ->
                if (user == null) return@flatMapLatest MutableStateFlow(GoalsUiState())
                combine(
                    getGoalsUseCase(user.id),
                    getProfileUseCase.observe(user.id),
                    getTransactionsUseCase(user.id),
                    getBudgetsUseCase(user.id)
                ) { goals, profile, transactions, budgets ->
                    val net = transactions.filter { it.affectsBalance }.sumOf {
                        if (it.type == TransactionType.INCOME) it.amount else -it.amount
                    }
                    val balance = profile?.currentBalance?.plus(net)
                    val safeToSpend = balance?.let { calculateSafeToSpend(it, calculateBudgetStatuses(budgets, transactions)) }
                    GoalsUiState(goals = goals, currentSafeToSpend = safeToSpend)
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Creating a goal never reserves money, changes balance, or writes a
    // transaction - it's a plain record, per the Phase 4 decision.
    fun addGoal(name: String, targetAmount: Double, currentSaved: Double, targetDate: Long?) {
        val userId = currentUserId ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            reportIfError(
                addGoalUseCase(
                    Goal(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = name,
                        targetAmount = targetAmount,
                        currentSaved = currentSaved,
                        targetDate = targetDate,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            )
        }
    }

    fun updateGoal(original: Goal, name: String, targetAmount: Double, currentSaved: Double, targetDate: Long?) {
        viewModelScope.launch {
            reportIfError(
                updateGoalUseCase(
                    original.copy(
                        name = name,
                        targetAmount = targetAmount,
                        currentSaved = currentSaved,
                        targetDate = targetDate,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            reportIfError(deleteGoalUseCase(id))
        }
    }

    private fun reportIfError(result: AppResult<Unit>) {
        if (result is AppResult.Error) {
            _errorMessage.value = result.message
        }
    }
}
