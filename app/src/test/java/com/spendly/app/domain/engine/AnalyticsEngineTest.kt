package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.model.SubscriptionFrequency
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AnalyticsEngineTest {

    private fun transaction(
        amount: Double,
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: String? = null,
        status: VerificationStatus = VerificationStatus.MANUAL_ENTRY,
        transactionDate: Long
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

    private fun subscription(
        amount: Double,
        nextPaymentDate: Long,
        isActive: Boolean = true
    ) = Subscription(
        id = "s-${System.nanoTime()}",
        userId = "u1",
        name = "Test",
        amount = amount,
        frequency = SubscriptionFrequency.MONTHLY,
        nextPaymentDate = nextPaymentDate,
        categoryId = null,
        isActive = isActive,
        updatedAt = 0L
    )

    private fun goal(targetAmount: Double, currentSaved: Double, targetDate: Long?) = Goal(
        id = "g-${System.nanoTime()}",
        userId = "u1",
        name = "Test goal",
        targetAmount = targetAmount,
        currentSaved = currentSaved,
        targetDate = targetDate,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun monthStart(monthsAgo: Int = 0): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, -monthsAgo)
    }.timeInMillis

    @Test
    fun `financial summary excludes failed and unknown transactions`() {
        val start = monthStart()
        val mid = start + TimeUnit.DAYS.toMillis(2)
        val transactions = listOf(
            transaction(1000.0, type = TransactionType.INCOME, status = VerificationStatus.MANUAL_ENTRY, transactionDate = mid),
            transaction(200.0, status = VerificationStatus.LAUNCHED_SUCCESS, transactionDate = mid),
            transaction(5000.0, status = VerificationStatus.LAUNCHED_FAILED, transactionDate = mid),
            transaction(5000.0, status = VerificationStatus.LAUNCHED_UNKNOWN, transactionDate = mid)
        )

        val summary = calculateFinancialSummary(transactions, start, monthStart(-1))

        assertEquals(1000.0, summary.income, 0.001)
        assertEquals(200.0, summary.expenses, 0.001)
        assertEquals(800.0, summary.savings, 0.001)
    }

    @Test
    fun `financial summary excludes out-of-range dates`() {
        val start = monthStart()
        val end = monthStart(-1)
        val lastMonth = start - TimeUnit.DAYS.toMillis(5)
        val transactions = listOf(
            transaction(100.0, transactionDate = start + TimeUnit.DAYS.toMillis(1)),
            transaction(999.0, transactionDate = lastMonth)
        )

        val summary = calculateFinancialSummary(transactions, start, end)

        assertEquals(100.0, summary.expenses, 0.001)
    }

    @Test
    fun `savings rate is zero when income is zero`() {
        val start = monthStart()
        val summary = calculateFinancialSummary(
            listOf(transaction(500.0, transactionDate = start + 1)),
            start,
            monthStart(-1)
        )

        assertEquals(0.0, summary.savingsRate, 0.001)
    }

    @Test
    fun `category breakdown computes percent of total expense`() {
        val start = monthStart()
        val end = monthStart(-1)
        val mid = start + TimeUnit.DAYS.toMillis(1)
        val categories = listOf(Category(id = "c1", userId = "u1", name = "Food", isDefault = true))
        val transactions = listOf(
            transaction(300.0, categoryId = "c1", transactionDate = mid),
            transaction(100.0, categoryId = null, transactionDate = mid)
        )

        val breakdown = calculateCategoryBreakdown(transactions, categories, start, end)

        val food = breakdown.first { it.categoryName == "Food" }
        val uncategorized = breakdown.first { it.categoryName == "Uncategorized" }
        assertEquals(75.0, food.percentOfTotal, 0.001)
        assertEquals(25.0, uncategorized.percentOfTotal, 0.001)
    }

    @Test
    fun `category breakdown is empty for no expenses`() {
        val start = monthStart()
        val breakdown = calculateCategoryBreakdown(emptyList(), emptyList(), start, monthStart(-1))

        assertTrue(breakdown.isEmpty())
    }

    @Test
    fun `month over month compares current calendar month against previous`() {
        val currentMonthMid = monthStart() + TimeUnit.DAYS.toMillis(2)
        val previousMonthMid = monthStart(1) + TimeUnit.DAYS.toMillis(2)
        val transactions = listOf(
            transaction(500.0, transactionDate = currentMonthMid),
            transaction(200.0, transactionDate = previousMonthMid)
        )

        val comparison = calculateMonthOverMonth(transactions, monthAnchor = System.currentTimeMillis())

        assertEquals(500.0, comparison.currentExpenses, 0.001)
        assertEquals(200.0, comparison.previousExpenses, 0.001)
        assertEquals(150.0, comparison.expenseDeltaPercent!!, 0.001)
    }

    @Test
    fun `month over month delta is null with no previous month baseline`() {
        val currentMonthMid = monthStart() + TimeUnit.DAYS.toMillis(2)
        val comparison = calculateMonthOverMonth(
            listOf(transaction(500.0, transactionDate = currentMonthMid)),
            monthAnchor = System.currentTimeMillis()
        )

        assertNull(comparison.expenseDeltaPercent)
    }

    @Test
    fun `subscriptionsDueInRange excludes canceled and out-of-range subscriptions`() {
        val now = System.currentTimeMillis()
        val rangeStart = now
        val rangeEnd = now + TimeUnit.DAYS.toMillis(30)
        val dueSoon = subscription(amount = 500.0, nextPaymentDate = now + TimeUnit.DAYS.toMillis(10))
        val dueLater = subscription(amount = 999.0, nextPaymentDate = now + TimeUnit.DAYS.toMillis(60))
        val canceled = subscription(amount = 999.0, nextPaymentDate = now + TimeUnit.DAYS.toMillis(1), isActive = false)

        val total = subscriptionsDueInRange(listOf(dueSoon, dueLater, canceled), rangeStart, rangeEnd)

        assertEquals(500.0, total, 0.001)
    }

    @Test
    fun `goalContributionsTotal sums required monthly saving and ignores goals without a target date`() {
        val now = System.currentTimeMillis()
        val goalWithDate = goal(targetAmount = 12000.0, currentSaved = 0.0, targetDate = now + TimeUnit.DAYS.toMillis(365))
        val goalWithoutDate = goal(targetAmount = 5000.0, currentSaved = 0.0, targetDate = null)

        val total = goalContributionsTotal(listOf(goalWithDate, goalWithoutDate))

        assertEquals(1000.0, total, 0.001)
    }

    @Test
    fun `financial summary never includes subscription or goal amounts`() {
        val start = monthStart()
        val end = monthStart(-1)
        val mid = start + TimeUnit.DAYS.toMillis(1)
        val transactions = listOf(transaction(100.0, transactionDate = mid))

        val summary = calculateFinancialSummary(transactions, start, end)
        val subscriptionTotal = subscriptionsDueInRange(listOf(subscription(9999.0, nextPaymentDate = mid)), start, end)
        val goalTotal = goalContributionsTotal(listOf(goal(9999.0, 0.0, targetDate = start + TimeUnit.DAYS.toMillis(30))))

        assertEquals(100.0, summary.expenses, 0.001)
        assertTrue(subscriptionTotal > 0.0)
        assertTrue(goalTotal > 0.0)
    }
}
