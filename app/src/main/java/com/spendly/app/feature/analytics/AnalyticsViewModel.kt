package com.spendly.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.affectsBalance
import com.spendly.app.domain.usecase.GetCategoriesUseCase
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
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
import java.util.Calendar
import javax.inject.Inject

data class CategoryTotal(val categoryName: String, val amount: Double, val fraction: Float)

data class AnalyticsUiState(
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val savings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    getCurrentUserUseCase: GetCurrentUserUseCase,
    getTransactionsUseCase: GetTransactionsUseCase,
    getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        getCurrentUserUseCase.currentUser
            .flatMapLatest { user ->
                if (user == null) return@flatMapLatest MutableStateFlow(AnalyticsUiState())
                combine(
                    getTransactionsUseCase(user.id),
                    getCategoriesUseCase(user.id)
                ) { transactions, categories -> buildUiState(transactions, categories) }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    private fun buildUiState(
        transactions: List<Transaction>,
        categories: List<Category>
    ): AnalyticsUiState {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thisMonth = transactions.filter { it.affectsBalance && it.transactionDate >= monthStart }
        val income = thisMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses = thisMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savings = income - expenses
        val savingsRate = if (income > 0) (savings / income) * 100 else 0.0

        val categoryMap = categories.associateBy { it.id }
        val totalsByCategory = thisMonth
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { categoryMap[it.categoryId]?.name ?: "Uncategorized" }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        val maxAmount = totalsByCategory.maxOfOrNull { it.second } ?: 0.0
        val categoryTotals = totalsByCategory.map { (name, amount) ->
            CategoryTotal(
                categoryName = name,
                amount = amount,
                fraction = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f
            )
        }

        return AnalyticsUiState(
            income = income,
            expenses = expenses,
            savings = savings,
            savingsRate = savingsRate,
            categoryTotals = categoryTotals
        )
    }
}
