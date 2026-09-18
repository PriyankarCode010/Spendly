package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Goal
import java.util.Calendar

/**
 * Deal planner calculation (V2 spec section 23): how much needs to be saved
 * per month to hit the target by the target date. Null targetDate means we
 * can't give a required monthly figure - the UI must say so, not guess.
 *
 * This is a pure read: creating or viewing a goal never reserves money,
 * changes balance, writes a transaction, or feeds into safe-to-spend. See
 * isGoalAffordable below for the one place a goal touches that number, and
 * even there it's advisory-only, never a side effect.
 */
fun requiredMonthlySaving(goal: Goal): Double? {
    val targetDate = goal.targetDate ?: return null
    val remaining = (goal.targetAmount - goal.currentSaved).coerceAtLeast(0.0)
    val monthsLeft = monthsBetween(System.currentTimeMillis(), targetDate).coerceAtLeast(1)
    return remaining / monthsLeft
}

/**
 * Advisory affordability indicator only (per Phase 4 decision): does the
 * required monthly saving fit within current safe-to-spend? This never
 * reserves anything and is recomputed fresh every time, never cached
 * against the goal record.
 */
fun isGoalAffordable(requiredMonthly: Double?, safeToSpend: Double?): Boolean? {
    if (requiredMonthly == null || safeToSpend == null) return null
    return requiredMonthly <= safeToSpend
}

private fun monthsBetween(fromMillis: Long, toMillis: Long): Int {
    val from = Calendar.getInstance().apply { timeInMillis = fromMillis }
    val to = Calendar.getInstance().apply { timeInMillis = toMillis }
    val yearsDiff = to.get(Calendar.YEAR) - from.get(Calendar.YEAR)
    val monthsDiff = to.get(Calendar.MONTH) - from.get(Calendar.MONTH)
    return yearsDiff * 12 + monthsDiff
}
