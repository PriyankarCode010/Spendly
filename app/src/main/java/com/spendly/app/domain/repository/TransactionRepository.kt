package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(userId: String): Flow<List<Transaction>>
    suspend fun getById(id: String): Transaction?
    suspend fun addTransaction(transaction: Transaction): AppResult<Unit>
    suspend fun updateTransaction(transaction: Transaction): AppResult<Unit>
    suspend fun deleteTransaction(id: String): AppResult<Unit>
}
