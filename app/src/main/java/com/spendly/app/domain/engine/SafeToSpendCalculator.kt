package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.BudgetPeriod
import com.spendly.app.domain.model.BudgetPriority
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.affectsBalance
import java.util.Calendar

data class BudgetStatus(
    val budget: Budget,
    val spent: Double,
    val remaining: Double
)

enum class BudgetWarningLevel {
    NORMAL,
    APPROACHING,
    OVER_BUDGET
}

enum class DashboardWarningLevel {
    NORMAL,
    WARNING,
    CRITICAL
}

private const val APPROACHING_THRESHOLD = 0.8

/**
 * Deterministic financial engine (see V2 spec section 16): this is the only
 * place safe-to-spend is computed, and it never involves AI. The UI/AI layer
 * may only read this result and explain it - never recompute or override it.
 *
 * Money already spent from a HIGH/MEDIUM priority budget has already left
 * the balance, so only the *unspent* portion of those budgets needs to stay
 * reserved. LOW priority budgets don't reduce safe-to-spend at all.
 */
fun calculateSafeToSpend(balance: Double, budgetStatuses: List<BudgetStatus>): Double {
    val reserved = reservedAmount(budgetStatuses)
    return balance - reserved
}

/**
 * Advisory only (V2 spec section 18): the app cannot enforce this, only
 * surface it. NORMAL means the balance covers every HIGH+MEDIUM reserve.
 * WARNING means discretionary/Low spending has started consuming Medium's
 * reserve, but High is still intact. CRITICAL means High itself - rent,
 * emergency fund, etc. - is now short.
 */
fun calculateDashboardWarningLevel(balance: Double, budgetStatuses: List<BudgetStatus>): DashboardWarningLevel {
    val highReserved = reservedAmountFor(budgetStatuses, BudgetPriority.HIGH)
    val mediumReserved = reservedAmountFor(budgetStatuses, BudgetPriority.MEDIUM)
    return when {
        balance >= highReserved + mediumReserved -> DashboardWarningLevel.NORMAL
        balance >= highReserved -> DashboardWarningLevel.WARNING
        else -> DashboardWarningLevel.CRITICAL
    }
}

/** Per-budget consumption state, independent of priority. */
fun calculateBudgetWarningLevel(status: BudgetStatus): BudgetWarningLevel {
    if (status.budget.amount <= 0.0) return BudgetWarningLevel.NORMAL
    val ratio = status.spent / status.budget.amount
    return when {
        status.remaining < 0 -> BudgetWarningLevel.OVER_BUDGET
        ratio >= APPROACHING_THRESHOLD -> BudgetWarningLevel.APPROACHING
        else -> BudgetWarningLevel.NORMAL
    }
}

fun calculateBudgetStatuses(budgets: List<Budget>, transactions: List<Transaction>): List<BudgetStatus> {
    val confirmedExpenses = transactions.filter { it.affectsBalance && it.type == TransactionType.EXPENSE }

    return budgets.map { budget ->
        val periodStart = currentPeriodStartMillis(budget.period)
        val spent = confirmedExpenses
            .filter { it.categoryId == budget.categoryId && it.transactionDate >= periodStart }
            .sumOf { it.amount }
        BudgetStatus(budget = budget, spent = spent, remaining = budget.amount - spent)
    }
}

private fun reservedAmount(budgetStatuses: List<BudgetStatus>): Double =
    budgetStatuses
        .filter { it.budget.priority == BudgetPriority.HIGH || it.budget.priority == BudgetPriority.MEDIUM }
        .sumOf { it.remaining.coerceAtLeast(0.0) }

private fun reservedAmountFor(budgetStatuses: List<BudgetStatus>, priority: BudgetPriority): Double =
    budgetStatuses
        .filter { it.budget.priority == priority }
        .sumOf { it.remaining.coerceAtLeast(0.0) }

private fun currentPeriodStartMillis(period: BudgetPeriod): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    when (period) {
        BudgetPeriod.WEEKLY -> calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        BudgetPeriod.MONTHLY -> calendar.set(Calendar.DAY_OF_MONTH, 1)
        BudgetPeriod.YEARLY -> calendar.set(Calendar.DAY_OF_YEAR, 1)
    }
    return calendar.timeInMillis
}
