package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.BudgetPeriod
import com.spendly.app.domain.model.BudgetPriority
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SafeToSpendCalculatorTest {

    private fun budget(
        id: String,
        categoryId: String,
        amount: Double,
        priority: BudgetPriority,
        period: BudgetPeriod = BudgetPeriod.MONTHLY
    ) = Budget(
        id = id,
        userId = "u1",
        categoryId = categoryId,
        amount = amount,
        priority = priority,
        period = period,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun transaction(
        categoryId: String,
        amount: Double,
        type: TransactionType = TransactionType.EXPENSE,
        status: VerificationStatus = VerificationStatus.MANUAL_ENTRY,
        transactionDate: Long = System.currentTimeMillis()
    ) = Transaction(
        id = "t-${System.nanoTime()}",
        userId = "u1",
        type = type,
        amount = amount,
        categoryId = categoryId,
        merchant = "Test",
        description = "",
        transactionDate = transactionDate,
        verificationStatus = status,
        createdAt = transactionDate,
        updatedAt = transactionDate
    )

    @Test
    fun `safe to spend reserves unspent high and medium priority budgets only`() {
        val statuses = listOf(
            BudgetStatus(budget("b1", "food", 10000.0, BudgetPriority.HIGH), spent = 0.0, remaining = 10000.0),
            BudgetStatus(budget("b2", "transport", 5000.0, BudgetPriority.MEDIUM), spent = 0.0, remaining = 5000.0),
            BudgetStatus(budget("b3", "shopping", 2000.0, BudgetPriority.LOW), spent = 0.0, remaining = 2000.0)
        )

        val safeToSpend = calculateSafeToSpend(balance = 20000.0, budgetStatuses = statuses)

        // LOW priority's remaining must not be reserved.
        assertEquals(5000.0, safeToSpend, 0.001)
    }

    @Test
    fun `overspent budget does not add to safe to spend`() {
        val statuses = listOf(
            BudgetStatus(budget("b1", "food", 5000.0, BudgetPriority.HIGH), spent = 7000.0, remaining = -2000.0)
        )

        val safeToSpend = calculateSafeToSpend(balance = 10000.0, budgetStatuses = statuses)

        // A negative "remaining" (overspent) must be coerced to 0, not subtracted as a bonus.
        assertEquals(10000.0, safeToSpend, 0.001)
    }

    @Test
    fun `budget spent excludes failed and unknown transactions`() {
        val budgets = listOf(budget("b1", "food", 5000.0, BudgetPriority.MEDIUM))
        val transactions = listOf(
            transaction("food", 1000.0, status = VerificationStatus.MANUAL_ENTRY),
            transaction("food", 2000.0, status = VerificationStatus.LAUNCHED_SUCCESS),
            transaction("food", 5000.0, status = VerificationStatus.LAUNCHED_FAILED),
            transaction("food", 5000.0, status = VerificationStatus.LAUNCHED_UNKNOWN)
        )

        val statuses = calculateBudgetStatuses(budgets, transactions)

        assertEquals(3000.0, statuses.single().spent, 0.001)
    }

    @Test
    fun `budget spent excludes income and out-of-month transactions`() {
        val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        val budgets = listOf(budget("b1", "food", 5000.0, BudgetPriority.MEDIUM))
        val transactions = listOf(
            transaction("food", 1000.0, type = TransactionType.EXPENSE),
            transaction("food", 9999.0, type = TransactionType.INCOME),
            transaction("food", 9999.0, transactionDate = lastMonth)
        )

        val statuses = calculateBudgetStatuses(budgets, transactions)

        assertEquals(1000.0, statuses.single().spent, 0.001)
    }

    @Test
    fun `weekly budget excludes last week transactions`() {
        val lastWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -8) }.timeInMillis
        val budgets = listOf(budget("b1", "food", 1000.0, BudgetPriority.LOW, BudgetPeriod.WEEKLY))
        val transactions = listOf(
            transaction("food", 200.0),
            transaction("food", 500.0, transactionDate = lastWeek)
        )

        val statuses = calculateBudgetStatuses(budgets, transactions)

        assertEquals(200.0, statuses.single().spent, 0.001)
    }

    @Test
    fun `yearly budget includes transactions from earlier this year`() {
        val earlierThisYear = Calendar.getInstance().apply { set(Calendar.MONTH, Calendar.JANUARY); set(Calendar.DAY_OF_MONTH, 2) }.timeInMillis
        val budgets = listOf(budget("b1", "food", 50000.0, BudgetPriority.LOW, BudgetPeriod.YEARLY))
        val transactions = listOf(transaction("food", 300.0, transactionDate = earlierThisYear))

        val statuses = calculateBudgetStatuses(budgets, transactions)

        assertEquals(300.0, statuses.single().spent, 0.001)
    }

    @Test
    fun `dashboard warning level is normal when balance covers all reserves`() {
        val statuses = listOf(
            BudgetStatus(budget("b1", "food", 10000.0, BudgetPriority.HIGH), spent = 0.0, remaining = 10000.0),
            BudgetStatus(budget("b2", "transport", 5000.0, BudgetPriority.MEDIUM), spent = 0.0, remaining = 5000.0)
        )

        assertEquals(DashboardWarningLevel.NORMAL, calculateDashboardWarningLevel(20000.0, statuses))
    }

    @Test
    fun `dashboard warning level is warning when medium reserve is encroached but high is safe`() {
        val statuses = listOf(
            BudgetStatus(budget("b1", "food", 10000.0, BudgetPriority.HIGH), spent = 0.0, remaining = 10000.0),
            BudgetStatus(budget("b2", "transport", 5000.0, BudgetPriority.MEDIUM), spent = 0.0, remaining = 5000.0)
        )

        assertEquals(DashboardWarningLevel.WARNING, calculateDashboardWarningLevel(12000.0, statuses))
    }

    @Test
    fun `dashboard warning level is critical when high reserve itself is short`() {
        val statuses = listOf(
            BudgetStatus(budget("b1", "food", 10000.0, BudgetPriority.HIGH), spent = 0.0, remaining = 10000.0),
            BudgetStatus(budget("b2", "transport", 5000.0, BudgetPriority.MEDIUM), spent = 0.0, remaining = 5000.0)
        )

        assertEquals(DashboardWarningLevel.CRITICAL, calculateDashboardWarningLevel(8000.0, statuses))
    }

    @Test
    fun `budget warning level thresholds`() {
        val b = budget("b1", "food", 1000.0, BudgetPriority.LOW)
        assertEquals(BudgetWarningLevel.NORMAL, calculateBudgetWarningLevel(BudgetStatus(b, 700.0, 300.0)))
        assertEquals(BudgetWarningLevel.APPROACHING, calculateBudgetWarningLevel(BudgetStatus(b, 800.0, 200.0)))
        assertEquals(BudgetWarningLevel.APPROACHING, calculateBudgetWarningLevel(BudgetStatus(b, 1000.0, 0.0)))
        assertEquals(BudgetWarningLevel.OVER_BUDGET, calculateBudgetWarningLevel(BudgetStatus(b, 1200.0, -200.0)))
    }
}
