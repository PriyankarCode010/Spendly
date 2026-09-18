package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.affectsBalance
import java.util.Calendar

private const val UNCATEGORIZED_LABEL = "Uncategorized"

data class FinancialSummary(
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val savings: Double = 0.0,
    val savingsRate: Double = 0.0
)

data class CategoryTotal(
    val categoryName: String,
    val amount: Double,
    val percentOfTotal: Double
)

/**
 * Current vs previous CALENDAR month (not a rolling 30-day window), matching
 * the calendar-month convention already used for MONTHLY budgets. A null
 * delta percent means there is no previous-month baseline to compare
 * against - never reported as 0%, which would falsely imply no change.
 */
data class MonthComparison(
    val currentIncome: Double,
    val currentExpenses: Double,
    val previousIncome: Double,
    val previousExpenses: Double,
    val expenseDeltaPercent: Double?
)

/**
 * Deterministic financial engine (V2 spec section 16): every calculation
 * here is a pure function over already-verified data. AI never computes any
 * of these numbers - it may only read and explain the results.
 *
 * Actual spending (this file), future commitments (subscriptions), plans
 * (goals), budget limits (SafeToSpendCalculator.calculateBudgetStatuses),
 * and safe-to-spend are five separate outputs and must never be merged into
 * one total. Budget-vs-actual reuses calculateBudgetStatuses directly - this
 * file does not reimplement budget-spent math.
 */
fun calculateFinancialSummary(
    transactions: List<Transaction>,
    rangeStart: Long,
    rangeEnd: Long
): FinancialSummary {
    val inRange = transactions.filter {
        it.affectsBalance && it.transactionDate >= rangeStart && it.transactionDate < rangeEnd
    }
    val income = inRange.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expenses = inRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val savings = income - expenses
    val savingsRate = if (income > 0) (savings / income) * 100 else 0.0
    return FinancialSummary(income, expenses, savings, savingsRate)
}

fun calculateCategoryBreakdown(
    transactions: List<Transaction>,
    categories: List<Category>,
    rangeStart: Long,
    rangeEnd: Long
): List<CategoryTotal> {
    val expensesInRange = transactions.filter {
        it.affectsBalance && it.type == TransactionType.EXPENSE &&
            it.transactionDate >= rangeStart && it.transactionDate < rangeEnd
    }
    val totalExpenses = expensesInRange.sumOf { it.amount }
    val categoryMap = categories.associateBy { it.id }

    return expensesInRange
        .groupBy { categoryMap[it.categoryId]?.name ?: UNCATEGORIZED_LABEL }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .map { (name, amount) ->
            CategoryTotal(
                categoryName = name,
                amount = amount,
                percentOfTotal = if (totalExpenses > 0) (amount / totalExpenses) * 100 else 0.0
            )
        }
}

fun calculateMonthOverMonth(transactions: List<Transaction>, monthAnchor: Long): MonthComparison {
    val currentStart = monthStartMillis(monthAnchor)
    val currentEnd = monthStartMillis(currentStart, offsetMonths = 1)
    val previousStart = monthStartMillis(currentStart, offsetMonths = -1)
    val previousEnd = currentStart

    val current = calculateFinancialSummary(transactions, currentStart, currentEnd)
    val previous = calculateFinancialSummary(transactions, previousStart, previousEnd)

    val expenseDeltaPercent = if (previous.expenses > 0) {
        ((current.expenses - previous.expenses) / previous.expenses) * 100
    } else {
        null
    }

    return MonthComparison(
        currentIncome = current.income,
        currentExpenses = current.expenses,
        previousIncome = previous.income,
        previousExpenses = previous.expenses,
        expenseDeltaPercent = expenseDeltaPercent
    )
}

/**
 * Forward-looking forecast only, scoped to an explicit calendar range chosen
 * on the Analytics screen - kept separate from UpcomingCommitments'
 * now-anchored "next N days" semantics used on the Dashboard. Never counted
 * as an actual expense.
 */
fun subscriptionsDueInRange(subscriptions: List<Subscription>, rangeStart: Long, rangeEnd: Long): Double =
    subscriptions
        .filter { it.isActive }
        .filter { nextOccurrenceAfter(it, rangeStart) in rangeStart..rangeEnd }
        .sumOf { it.amount }

/**
 * Plan visibility only - always a flat monthly figure, never prorated to the
 * selected range length (same ongoing-obligation semantics as GoalPlanner).
 * Never counted as an actual expense.
 */
fun goalContributionsTotal(goals: List<Goal>): Double =
    goals.mapNotNull { requiredMonthlySaving(it) }.sum()

private fun monthStartMillis(anchor: Long, offsetMonths: Int = 0): Long =
    Calendar.getInstance().apply {
        timeInMillis = anchor
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, offsetMonths)
    }.timeInMillis
