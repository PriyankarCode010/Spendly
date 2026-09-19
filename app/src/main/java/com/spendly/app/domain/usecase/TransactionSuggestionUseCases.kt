package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.engine.notification.ParsedSuggestion
import com.spendly.app.domain.model.SuggestionStatus
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSuggestion
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.VerificationStatus
import com.spendly.app.domain.repository.TransactionSuggestionRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetPendingSuggestionsUseCase @Inject constructor(private val repository: TransactionSuggestionRepository) {
    operator fun invoke(userId: String): Flow<List<TransactionSuggestion>> = repository.observePending(userId)
}

class GetAllSuggestionsUseCase @Inject constructor(private val repository: TransactionSuggestionRepository) {
    operator fun invoke(userId: String): Flow<List<TransactionSuggestion>> = repository.observeAll(userId)
}

/** Called only from the notification listener - never creates a Transaction, only a PENDING suggestion (or a no-op if it's a duplicate notification of an existing pending one). */
class IngestNotificationSuggestionUseCase @Inject constructor(
    private val repository: TransactionSuggestionRepository
) {
    suspend operator fun invoke(
        userId: String,
        sourcePackage: String,
        parsed: ParsedSuggestion,
        notificationPostedAt: Long
    ): AppResult<Unit> = repository.ingest(
        TransactionSuggestion(
            id = UUID.randomUUID().toString(),
            userId = userId,
            sourcePackage = sourcePackage,
            amount = parsed.amount,
            direction = parsed.direction,
            merchant = parsed.merchant,
            referenceId = parsed.referenceId,
            notificationPostedAt = notificationPostedAt,
            status = SuggestionStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            resultingTransactionId = null
        )
    )
}

/**
 * "Record anyway" / plain confirm path: creates a real Transaction tagged
 * OBSERVED_UNVERIFIED - never LAUNCHED_SUCCESS, since a notification is
 * never proof of a completed bank transaction - then marks the suggestion
 * CONFIRMED, linked to the new transaction.
 */
class ConfirmSuggestionUseCase @Inject constructor(
    private val repository: TransactionSuggestionRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) {
    suspend operator fun invoke(suggestion: TransactionSuggestion, categoryId: String?): AppResult<Unit> {
        val amount = suggestion.amount ?: return AppResult.Error("Amount is required to confirm this suggestion")
        val direction = suggestion.direction ?: return AppResult.Error("Debit/credit direction is required to confirm this suggestion")

        val now = System.currentTimeMillis()
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            userId = suggestion.userId,
            type = if (direction == TransactionDirection.DEBIT) TransactionType.EXPENSE else TransactionType.INCOME,
            amount = amount,
            categoryId = categoryId,
            merchant = suggestion.merchant ?: "Unknown",
            description = "Auto-detected",
            transactionDate = suggestion.notificationPostedAt,
            verificationStatus = VerificationStatus.OBSERVED_UNVERIFIED,
            createdAt = now,
            updatedAt = now
        )

        val addResult = addTransactionUseCase(transaction)
        if (addResult is AppResult.Error) return addResult

        return repository.markConfirmed(suggestion.id, transaction.id)
    }
}

/** "Use existing" path: links the suggestion to an already-recorded Transaction instead of creating a new one. */
class LinkSuggestionToExistingTransactionUseCase @Inject constructor(
    private val repository: TransactionSuggestionRepository
) {
    suspend operator fun invoke(suggestionId: String, existingTransactionId: String): AppResult<Unit> =
        repository.markConfirmed(suggestionId, existingTransactionId)
}

class DismissSuggestionUseCase @Inject constructor(private val repository: TransactionSuggestionRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.markDismissed(id)
}
