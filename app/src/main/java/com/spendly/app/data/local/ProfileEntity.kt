package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val currency: String,
    val monthlyIncome: Double,
    val currentBalance: Double,
    val updatedAt: Long
)
