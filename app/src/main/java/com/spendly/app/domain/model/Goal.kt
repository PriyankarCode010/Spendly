package com.spendly.app.domain.model

data class Goal(
    val id: String,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val currentSaved: Double,
    val targetDate: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
