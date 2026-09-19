package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.TransactionSuggestion
import kotlinx.coroutines.flow.Flow

interface TransactionSuggestionRepository {
    fun observePending(userId: String): Flow<List<TransactionSuggestion>>
    fun observeAll(userId: String): Flow<List<TransactionSuggestion>>

    /**
     * Skips inserting if isDuplicateSuggestion (see SuggestionDuplicateDetector)
     * matches an existing PENDING row for the same source/direction/amount
     * within a short window - never creates a Transaction, only ever a
     * suggestion row or a no-op.
     */
    suspend fun ingest(suggestion: TransactionSuggestion): AppResult<Unit>

    suspend fun markConfirmed(id: String, transactionId: String): AppResult<Unit>
    suspend fun markDismissed(id: String): AppResult<Unit>
}
