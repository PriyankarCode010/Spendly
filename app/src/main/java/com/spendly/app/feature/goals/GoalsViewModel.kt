package com.spendly.app.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.usecase.AddGoalUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val addGoalUseCase: AddGoalUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user -> user?.let { getGoalsUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
            .onEach { _goals.value = it }
            .launchIn(viewModelScope)
    }

    fun addGoal(name: String, targetAmount: Double, currentSaved: Double, targetDate: Long?) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            addGoalUseCase(
                Goal(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = name,
                    targetAmount = targetAmount,
                    currentSaved = currentSaved,
                    targetDate = targetDate,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}

/**
 * The "deal planner" calculation: how much needs to be saved per month to
 * hit the target by the target date. Null targetDate means we can't give a
 * required monthly figure - the UI must say so rather than guessing.
 */
fun requiredMonthlySaving(goal: Goal): Double? {
    val targetDate = goal.targetDate ?: return null
    val remaining = (goal.targetAmount - goal.currentSaved).coerceAtLeast(0.0)
    val monthsLeft = monthsBetween(System.currentTimeMillis(), targetDate).coerceAtLeast(1)
    return remaining / monthsLeft
}

private fun monthsBetween(fromMillis: Long, toMillis: Long): Int {
    val from = java.util.Calendar.getInstance().apply { timeInMillis = fromMillis }
    val to = java.util.Calendar.getInstance().apply { timeInMillis = toMillis }
    val yearsDiff = to.get(java.util.Calendar.YEAR) - from.get(java.util.Calendar.YEAR)
    val monthsDiff = to.get(java.util.Calendar.MONTH) - from.get(java.util.Calendar.MONTH)
    return yearsDiff * 12 + monthsDiff
}
