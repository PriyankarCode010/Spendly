package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetsUseCase @Inject constructor(private val repository: BudgetRepository) {
    operator fun invoke(userId: String): Flow<List<Budget>> = repository.observeAll(userId)
}

class GetBudgetByIdUseCase @Inject constructor(private val repository: BudgetRepository) {
    suspend operator fun invoke(id: String): Budget? = repository.getById(id)
}

class AddBudgetUseCase @Inject constructor(private val repository: BudgetRepository) {
    suspend operator fun invoke(budget: Budget): AppResult<Unit> = repository.addBudget(budget)
}

class UpdateBudgetUseCase @Inject constructor(private val repository: BudgetRepository) {
    suspend operator fun invoke(budget: Budget): AppResult<Unit> = repository.updateBudget(budget)
}

class DeleteBudgetUseCase @Inject constructor(private val repository: BudgetRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.deleteBudget(id)
}
