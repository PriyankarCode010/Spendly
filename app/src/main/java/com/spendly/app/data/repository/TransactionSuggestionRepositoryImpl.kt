package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.local.TransactionSuggestionDao
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.engine.isDuplicateSuggestion
import com.spendly.app.domain.model.SuggestionStatus
import com.spendly.app.domain.model.TransactionSuggestion
import com.spendly.app.domain.repository.TransactionSuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val DUPLICATE_LOOKUP_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5)

@Singleton
class TransactionSuggestionRepositoryImpl @Inject constructor(
    private val suggestionDao: TransactionSuggestionDao
) : TransactionSuggestionRepository {

    override fun observePending(userId: String): Flow<List<TransactionSuggestion>> =
        suggestionDao.observePending(userId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(userId: String): Flow<List<TransactionSuggestion>> =
        suggestionDao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun ingest(suggestion: TransactionSuggestion): AppResult<Unit> = runCatching {
        val nearbyPending = suggestionDao.findPendingInWindow(
            userId = suggestion.userId,
            sourcePackage = suggestion.sourcePackage,
            fromMillis = suggestion.notificationPostedAt - DUPLICATE_LOOKUP_WINDOW_MILLIS,
            toMillis = suggestion.notificationPostedAt + DUPLICATE_LOOKUP_WINDOW_MILLIS
        ).map { it.toDomain() }

        if (!isDuplicateSuggestion(suggestion, nearbyPending)) {
            suggestionDao.insert(suggestion.toEntity())
        }
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to record suggestion", it) }
    )

    override suspend fun markConfirmed(id: String, transactionId: String): AppResult<Unit> = runCatching {
        val existing = suggestionDao.getById(id) ?: error("Suggestion not found")
        suggestionDao.update(existing.copy(status = SuggestionStatus.CONFIRMED.name, resultingTransactionId = transactionId))
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to confirm suggestion", it) }
    )

    override suspend fun markDismissed(id: String): AppResult<Unit> = runCatching {
        val existing = suggestionDao.getById(id) ?: error("Suggestion not found")
        suggestionDao.update(existing.copy(status = SuggestionStatus.DISMISSED.name))
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to dismiss suggestion", it) }
    )
}
