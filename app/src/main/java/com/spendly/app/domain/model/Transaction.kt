package com.spendly.app.domain.model

data class Transaction(
    val id: String,
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val categoryId: String?,
    val merchant: String,
    val description: String,
    val transactionDate: Long,
    val verificationStatus: VerificationStatus,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Whether this transaction represents money that's confirmed to have moved.
 * A LAUNCHED_UNKNOWN or LAUNCHED_FAILED payment must never affect balance,
 * budget-spent, or analytics totals - we don't yet know (or know it didn't
 * happen) that the money actually moved. Every aggregation over amounts
 * must filter on this first.
 */
val Transaction.affectsBalance: Boolean
    get() = verificationStatus != VerificationStatus.LAUNCHED_FAILED &&
        verificationStatus != VerificationStatus.LAUNCHED_UNKNOWN
