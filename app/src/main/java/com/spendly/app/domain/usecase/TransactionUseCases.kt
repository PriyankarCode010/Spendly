package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(private val repository: TransactionRepository) {
    operator fun invoke(userId: String): Flow<List<Transaction>> = repository.observeAll(userId)
}

class GetTransactionByIdUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: String): Transaction? = repository.getById(id)
}

class AddTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction): AppResult<Unit> = repository.addTransaction(transaction)
}

class UpdateTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction): AppResult<Unit> = repository.updateTransaction(transaction)
}

class DeleteTransactionUseCase @Inject constructor(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.deleteTransaction(id)
}
