package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(userId: String): Flow<List<Budget>>
    suspend fun getById(id: String): Budget?
    suspend fun addBudget(budget: Budget): AppResult<Unit>
    suspend fun updateBudget(budget: Budget): AppResult<Unit>
    suspend fun deleteBudget(id: String): AppResult<Unit>
}
