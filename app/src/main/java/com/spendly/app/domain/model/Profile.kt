package com.spendly.app.domain.model

data class Profile(
    val userId: String,
    val currency: String,
    val monthlyIncome: Double,
    val currentBalance: Double,
    val updatedAt: Long
)
