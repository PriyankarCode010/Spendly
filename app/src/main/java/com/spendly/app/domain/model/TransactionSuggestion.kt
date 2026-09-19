package com.spendly.app.domain.model

enum class TransactionDirection { DEBIT, CREDIT }

enum class SuggestionStatus { PENDING, CONFIRMED, DISMISSED }

/**
 * A best-effort candidate parsed from a payment app's notification - never a
 * Transaction, never proof anything actually happened. Only user
 * confirmation (see ConfirmSuggestionUseCase) turns this into a real
 * Transaction, tagged OBSERVED_UNVERIFIED. Raw notification text is never
 * stored here - only the specific fields Spendly needs.
 */
data class TransactionSuggestion(
    val id: String,
    val userId: String,
    val sourcePackage: String,
    val amount: Double?,
    val direction: TransactionDirection?,
    val merchant: String?,
    val referenceId: String?,
    val notificationPostedAt: Long,
    val status: SuggestionStatus,
    val createdAt: Long,
    val resultingTransactionId: String?
)
