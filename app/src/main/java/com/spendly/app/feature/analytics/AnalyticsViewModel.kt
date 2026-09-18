package com.spendly.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.engine.BudgetStatus
import com.spendly.app.domain.engine.CategoryTotal
import com.spendly.app.domain.engine.FinancialSummary
import com.spendly.app.domain.engine.MonthComparison
import com.spendly.app.domain.engine.calculateBudgetStatuses
import com.spendly.app.domain.engine.calculateCategoryBreakdown
import com.spendly.app.domain.engine.calculateFinancialSummary
import com.spendly.app.domain.engine.calculateMonthOverMonth
import com.spendly.app.domain.engine.goalContributionsTotal
import com.spendly.app.domain.engine.subscriptionsDueInRange
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.usecase.GetActiveSubscriptionsUseCase
import com.spendly.app.domain.usecase.GetBudgetsUseCase
import com.spendly.app.domain.usecase.GetCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.GetGoalsUseCase
import com.spendly.app.domain.usecase.GetTransactionsUseCase
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
import java.util.Calendar
import javax.inject.Inject

enum class AnalyticsPeriodType { MONTH, YEAR, CUSTOM }

data class AnalyticsUiState(
    val periodType: AnalyticsPeriodType = AnalyticsPeriodType.MONTH,
    val rangeStart: Long = 0L,
    val rangeEnd: Long = 0L,
    val summary: FinancialSummary = FinancialSummary(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val monthComparison: MonthComparison? = null,
    val budgetStatuses: List<BudgetStatus> = emptyList(),
    val upcomingSubscriptionAmount: Double = 0.0,
    val goalContributionAmount: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getActiveSubscriptionsUseCase: GetActiveSubscriptionsUseCase,
    private val getGoalsUseCase: GetGoalsUseCase
) : ViewModel() {

    private var currentUserId: String? = null

    private val _selectedRange = MutableStateFlow(monthRange(System.currentTimeMillis()))
    private val _periodType = MutableStateFlow(AnalyticsPeriodType.MONTH)

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = getCurrentUserUseCase.currentUser
        .flatMapLatest { user -> user?.let { getCategoriesUseCase(it.id) } ?: MutableStateFlow(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        getCurrentUserUseCase.currentUser
            .onEach { currentUserId = it?.id }
            .flatMapLatest { user ->
                if (user == null) return@flatMapLatest MutableStateFlow(EMPTY_DATA)
                combine(
                    getTransactionsUseCase(user.id),
                    getCategoriesUseCase(user.id),
                    getBudgetsUseCase(user.id),
                    getActiveSubscriptionsUseCase(user.id),
                    getGoalsUseCase(user.id)
                ) { transactions, categories, budgets, subscriptions, goals ->
                    Data(transactions, categories, budgets, subscriptions, goals)
                }
            }
            .combine(_selectedRange) { data, range -> data to range }
            .combine(_periodType) { (data, range), periodType ->
                buildUiState(data, range, periodType)
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    fun selectThisMonth() {
        _periodType.value = AnalyticsPeriodType.MONTH
        _selectedRange.value = monthRange(System.currentTimeMillis())
    }

    fun selectThisYear() {
        _periodType.value = AnalyticsPeriodType.YEAR
        _selectedRange.value = yearRange(System.currentTimeMillis())
    }

    fun selectCustomRange(start: Long, end: Long) {
        _periodType.value = AnalyticsPeriodType.CUSTOM
        _selectedRange.value = start to end
    }

    private fun buildUiState(
        data: Data,
        range: Pair<Long, Long>,
        periodType: AnalyticsPeriodType
    ): AnalyticsUiState {
        val (rangeStart, rangeEnd) = range
        val summary = calculateFinancialSummary(data.transactions, rangeStart, rangeEnd)
        val categoryTotals = calculateCategoryBreakdown(data.transactions, data.categories, rangeStart, rangeEnd)
        val monthComparison = if (periodType == AnalyticsPeriodType.MONTH) {
            calculateMonthOverMonth(data.transactions, rangeStart)
        } else {
            null
        }
        val budgetStatuses = calculateBudgetStatuses(data.budgets, data.transactions)
        val upcomingSubscriptionAmount = subscriptionsDueInRange(data.subscriptions, rangeStart, rangeEnd)
        val goalContributionAmount = goalContributionsTotal(data.goals)

        return AnalyticsUiState(
            periodType = periodType,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            summary = summary,
            categoryTotals = categoryTotals,
            monthComparison = monthComparison,
            budgetStatuses = budgetStatuses,
            upcomingSubscriptionAmount = upcomingSubscriptionAmount,
            goalContributionAmount = goalContributionAmount
        )
    }

    private data class Data(
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val budgets: List<Budget>,
        val subscriptions: List<Subscription>,
        val goals: List<Goal>
    )

    private companion object {
        val EMPTY_DATA = Data(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

private fun monthRange(anchor: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = anchor
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    return start.timeInMillis to end.timeInMillis
}

private fun yearRange(anchor: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = anchor
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
    return start.timeInMillis to end.timeInMillis
}
