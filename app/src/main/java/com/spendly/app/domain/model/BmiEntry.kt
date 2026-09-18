package com.spendly.app.domain.model

data class BmiEntry(
    val id: String,
    val userId: String,
    val heightCm: Double,
    val weightKg: Double,
    val recordedAt: Long
)
