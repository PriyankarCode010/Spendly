package com.spendly.app.data.mapper

import com.spendly.app.data.local.BudgetEntity
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.BudgetPeriod
import com.spendly.app.domain.model.BudgetPriority

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    userId = userId,
    categoryId = categoryId,
    amount = amount,
    priority = BudgetPriority.valueOf(priority),
    period = BudgetPeriod.valueOf(period),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    userId = userId,
    categoryId = categoryId,
    amount = amount,
    priority = priority.name,
    period = period.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)
