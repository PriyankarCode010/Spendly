package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.local.GoalDao
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override fun observeAll(userId: String): Flow<List<Goal>> =
        goalDao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Goal? =
        goalDao.getById(id)?.toDomain()

    override suspend fun addGoal(goal: Goal): AppResult<Unit> = runCatching {
        goalDao.insert(goal.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to save goal", it) }
    )

    override suspend fun updateGoal(goal: Goal): AppResult<Unit> = runCatching {
        goalDao.update(goal.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to update goal", it) }
    )

    override suspend fun deleteGoal(id: String): AppResult<Unit> = runCatching {
        goalDao.deleteById(id)
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to delete goal", it) }
    )
}
