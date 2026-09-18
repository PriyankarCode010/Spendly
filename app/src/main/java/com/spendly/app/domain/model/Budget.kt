package com.spendly.app.domain.model

data class Budget(
    val id: String,
    val userId: String,
    val categoryId: String?,
    val amount: Double,
    val priority: BudgetPriority,
    val period: BudgetPeriod,
    val createdAt: Long,
    val updatedAt: Long
)
