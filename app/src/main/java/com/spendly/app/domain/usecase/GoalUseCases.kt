package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.Goal
import com.spendly.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGoalsUseCase @Inject constructor(private val repository: GoalRepository) {
    operator fun invoke(userId: String): Flow<List<Goal>> = repository.observeAll(userId)
}

class AddGoalUseCase @Inject constructor(private val repository: GoalRepository) {
    suspend operator fun invoke(goal: Goal): AppResult<Unit> = repository.addGoal(goal)
}
