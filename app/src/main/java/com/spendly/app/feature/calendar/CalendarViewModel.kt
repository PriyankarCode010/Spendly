package com.spendly.app.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.model.CalendarExportSourceType
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.usecase.GetActiveSubscriptionsUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetExportedSourceIdsUseCase
import com.spendly.app.domain.usecase.GetGoalsUseCase
import com.spendly.app.domain.usecase.MarkCalendarExportedUseCase
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
import javax.inject.Inject

data class CalendarUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val exportedSubscriptionIds: Set<String> = emptySet(),
    val goals: List<Goal> = emptyList(),
    val exportedGoalIds: Set<String> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getActiveSubscriptionsUseCase: GetActiveSubscriptionsUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val getExportedSourceIdsUseCase: GetExportedSourceIdsUseCase,
    private val markCalendarExportedUseCase: MarkCalendarExportedUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user ->
                if (user == null) return@flatMapLatest MutableStateFlow(CalendarUiState())
                combine(
                    getActiveSubscriptionsUseCase(user.id),
                    getExportedSourceIdsUseCase(user.id, CalendarExportSourceType.SUBSCRIPTION),
                    getGoalsUseCase(user.id),
                    getExportedSourceIdsUseCase(user.id, CalendarExportSourceType.GOAL)
                ) { subscriptions, exportedSubs, goals, exportedGoals ->
                    CalendarUiState(
                        subscriptions = subscriptions,
                        exportedSubscriptionIds = exportedSubs,
                        goals = goals.filter { it.targetDate != null },
                        exportedGoalIds = exportedGoals
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun markSubscriptionExported(subscriptionId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            markCalendarExportedUseCase(userId, CalendarExportSourceType.SUBSCRIPTION, subscriptionId)
        }
    }

    fun markGoalExported(goalId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            markCalendarExportedUseCase(userId, CalendarExportSourceType.GOAL, goalId)
        }
    }
}
