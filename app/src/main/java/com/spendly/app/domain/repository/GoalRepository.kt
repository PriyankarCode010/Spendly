package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeAll(userId: String): Flow<List<Goal>>
    suspend fun addGoal(goal: Goal): AppResult<Unit>
}
