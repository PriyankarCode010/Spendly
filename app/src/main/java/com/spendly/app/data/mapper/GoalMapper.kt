package com.spendly.app.data.mapper

import com.spendly.app.data.local.GoalEntity
import com.spendly.app.domain.model.Goal

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    userId = userId,
    name = name,
    targetAmount = targetAmount,
    currentSaved = currentSaved,
    targetDate = targetDate,
    createdAt = createdAt
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    userId = userId,
    name = name,
    targetAmount = targetAmount,
    currentSaved = currentSaved,
    targetDate = targetDate,
    createdAt = createdAt
)
