package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.model.SubscriptionFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class UpcomingCommitmentsTest {

    private fun subscription(
        amount: Double,
        frequency: SubscriptionFrequency = SubscriptionFrequency.MONTHLY,
        nextPaymentDate: Long,
        isActive: Boolean = true
    ) = Subscription(
        id = "s-${System.nanoTime()}",
        userId = "u1",
        name = "Test",
        amount = amount,
        frequency = frequency,
        nextPaymentDate = nextPaymentDate,
        categoryId = null,
        isActive = isActive,
        updatedAt = 0L
    )

    private fun goal(
        targetAmount: Double,
        currentSaved: Double,
        targetDate: Long?
    ) = Goal(
        id = "g-${System.nanoTime()}",
        userId = "u1",
        name = "Test goal",
        targetAmount = targetAmount,
        currentSaved = currentSaved,
        targetDate = targetDate,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun `nextOccurrenceAfter returns stored date when it is still in the future`() {
        val now = 1_000_000L
        val future = now + TimeUnit.DAYS.toMillis(5)
        val sub = subscription(amount = 100.0, nextPaymentDate = future)

        assertEquals(future, nextOccurrenceAfter(sub, now))
    }

    @Test
    fun `nextOccurrenceAfter projects forward past due monthly subscriptions without mutating stored date`() {
        val now = System.currentTimeMillis()
        val overdueBy40Days = now - TimeUnit.DAYS.toMillis(40)
        val sub = subscription(amount = 100.0, frequency = SubscriptionFrequency.MONTHLY, nextPaymentDate = overdueBy40Days)

        val projected = nextOccurrenceAfter(sub, now)

        assertTrue("projected occurrence must be on/after now", projected >= now)
        assertEquals(overdueBy40Days, sub.nextPaymentDate)
    }

    @Test
    fun `calculateUpcomingCommitments sums only active subscriptions due within the window`() {
        val now = System.currentTimeMillis()
        val dueSoon = subscription(amount = 500.0, nextPaymentDate = now + TimeUnit.DAYS.toMillis(10))
        val dueLater = subscription(amount = 999.0, nextPaymentDate = now + TimeUnit.DAYS.toMillis(60))
        val canceled = subscription(amount = 999.0, nextPaymentDate = now + TimeUnit.DAYS.toMillis(1), isActive = false)

        val result = calculateUpcomingCommitments(
            subscriptions = listOf(dueSoon, dueLater, canceled),
            goals = emptyList(),
            now = now,
            windowDays = 30
        )

        assertEquals(500.0, result.subscriptionAmount, 0.001)
    }

    @Test
    fun `calculateUpcomingCommitments includes every goal's required monthly saving regardless of window`() {
        val now = System.currentTimeMillis()
        val farGoal = goal(targetAmount = 12000.0, currentSaved = 0.0, targetDate = now + TimeUnit.DAYS.toMillis(365))

        val result = calculateUpcomingCommitments(
            subscriptions = emptyList(),
            goals = listOf(farGoal),
            now = now,
            windowDays = 30
        )

        assertEquals(1000.0, result.goalContributionAmount, 0.001)
        assertEquals(1000.0, result.total, 0.001)
    }

    @Test
    fun `requiredMonthlySaving is null without a target date`() {
        val goalNoDate = goal(targetAmount = 5000.0, currentSaved = 0.0, targetDate = null)

        assertNull(requiredMonthlySaving(goalNoDate))
    }

    @Test
    fun `isGoalAffordable compares required monthly saving against safe to spend`() {
        assertEquals(true, isGoalAffordable(requiredMonthly = 1000.0, safeToSpend = 1500.0))
        assertEquals(false, isGoalAffordable(requiredMonthly = 2000.0, safeToSpend = 1500.0))
        assertNull(isGoalAffordable(requiredMonthly = null, safeToSpend = 1500.0))
        assertNull(isGoalAffordable(requiredMonthly = 1000.0, safeToSpend = null))
    }
}
