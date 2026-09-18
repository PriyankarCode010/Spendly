package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.local.TransactionDao
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun observeAll(userId: String): Flow<List<Transaction>> =
        transactionDao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun addTransaction(transaction: Transaction): AppResult<Unit> = runCatching {
        transactionDao.insert(transaction.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to save transaction", it) }
    )

    override suspend fun updateTransaction(transaction: Transaction): AppResult<Unit> = runCatching {
        transactionDao.update(transaction.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to update transaction", it) }
    )

    override suspend fun deleteTransaction(id: String): AppResult<Unit> = runCatching {
        transactionDao.deleteById(id)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to delete transaction", it) }
    )
}
