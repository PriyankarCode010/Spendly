package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.local.BudgetDao
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun observeAll(userId: String): Flow<List<Budget>> =
        budgetDao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Budget? =
        budgetDao.getById(id)?.toDomain()

    override suspend fun addBudget(budget: Budget): AppResult<Unit> = runCatching {
        val duplicate = budgetDao.findDuplicate(budget.userId, budget.categoryId, budget.period.name, excludeId = "")
        if (duplicate != null) error("A budget already exists for this category and period")
        budgetDao.insert(budget.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to save budget", it) }
    )

    override suspend fun updateBudget(budget: Budget): AppResult<Unit> = runCatching {
        val duplicate = budgetDao.findDuplicate(budget.userId, budget.categoryId, budget.period.name, excludeId = budget.id)
        if (duplicate != null) error("A budget already exists for this category and period")
        budgetDao.update(budget.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to update budget", it) }
    )

    override suspend fun deleteBudget(id: String): AppResult<Unit> = runCatching {
        budgetDao.deleteById(id)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to delete budget", it) }
    )
}
