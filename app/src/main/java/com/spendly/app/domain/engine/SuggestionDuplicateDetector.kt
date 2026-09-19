package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSuggestion
import com.spendly.app.domain.model.TransactionType
import java.util.concurrent.TimeUnit

private val DUPLICATE_SUGGESTION_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5)
private val DUPLICATE_TRANSACTION_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(10)
private const val AMOUNT_EPSILON = 0.01

/**
 * Two notifications for the same real-world payment (e.g. "processing" then
 * "successful") should not create two PENDING rows. This never touches an
 * existing financial Transaction - it only prevents suggestion-inbox noise,
 * so auto-skipping here is safe (V2 spec's "never auto-merge a financial
 * record" rule applies to Transactions, not to not-yet-confirmed
 * suggestions).
 */
fun isDuplicateSuggestion(candidate: TransactionSuggestion, existingPending: List<TransactionSuggestion>): Boolean =
    existingPending.any { existing ->
        existing.sourcePackage == candidate.sourcePackage &&
            existing.direction == candidate.direction &&
            amountsMatch(existing.amount, candidate.amount) &&
            kotlin.math.abs(existing.notificationPostedAt - candidate.notificationPostedAt) <= DUPLICATE_SUGGESTION_WINDOW_MILLIS
    }

/**
 * Fuzzy match only, never authoritative: the caller must surface this to
 * the user (Use existing / Record anyway / Dismiss) and must never
 * automatically merge, delete, or suppress the matched Transaction.
 */
fun findLikelyDuplicateTransaction(
    candidate: TransactionSuggestion,
    existingTransactions: List<Transaction>
): Transaction? = existingTransactions.firstOrNull { tx ->
    amountsMatch(tx.amount, candidate.amount) &&
        kotlin.math.abs(tx.transactionDate - candidate.notificationPostedAt) <= DUPLICATE_TRANSACTION_WINDOW_MILLIS &&
        (candidate.direction == null || (candidate.direction == TransactionDirection.DEBIT) == (tx.type == TransactionType.EXPENSE))
}

private fun amountsMatch(a: Double?, b: Double?): Boolean {
    if (a == null || b == null) return false
    return kotlin.math.abs(a - b) < AMOUNT_EPSILON
}
