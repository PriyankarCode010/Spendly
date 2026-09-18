package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.model.Subscription

data class UpcomingCommitments(
    val subscriptionAmount: Double,
    val goalContributionAmount: Double
) {
    val total: Double get() = subscriptionAmount + goalContributionAmount
}

/**
 * Forward-looking forecast only (V2 spec sections 21/23) - never touches
 * Budget/Transaction/safe-to-spend. A subscription due tomorrow or a goal's
 * required monthly saving is not spent money yet, so this must stay a
 * separate number the dashboard shows *alongside* safe-to-spend, never
 * merged into it.
 *
 * Subscriptions count if their next occurrence falls within the window.
 * Goal contributions are an ongoing monthly obligation, not a single dated
 * event, so every goal with a required monthly saving counts in full
 * regardless of window length.
 */
fun calculateUpcomingCommitments(
    subscriptions: List<Subscription>,
    goals: List<Goal>,
    now: Long,
    windowDays: Int
): UpcomingCommitments {
    val windowEnd = now + windowDays * MILLIS_PER_DAY

    val subscriptionAmount = subscriptions
        .filter { it.isActive }
        .filter { nextOccurrenceAfter(it, now) in now..windowEnd }
        .sumOf { it.amount }

    val goalContributionAmount = goals
        .mapNotNull { requiredMonthlySaving(it) }
        .sum()

    return UpcomingCommitments(subscriptionAmount, goalContributionAmount)
}

/**
 * Next time this subscription is due, on or after [now]. Never mutates the
 * stored nextPaymentDate - the stored value is the source of truth and only
 * advances when the user edits it; this just projects forward for display.
 */
fun nextOccurrenceAfter(subscription: Subscription, now: Long): Long {
    if (subscription.nextPaymentDate >= now) return subscription.nextPaymentDate
    val intervalMillis = (365.25 * MILLIS_PER_DAY / subscription.frequency.perYear).toLong()
    if (intervalMillis <= 0L) return subscription.nextPaymentDate
    val elapsed = now - subscription.nextPaymentDate
    val cyclesPassed = (elapsed / intervalMillis) + 1
    return subscription.nextPaymentDate + cyclesPassed * intervalMillis
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
