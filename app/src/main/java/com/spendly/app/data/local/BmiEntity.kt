package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bmi_entries", indices = [Index("userId")])
data class BmiEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val heightCm: Double,
    val weightKg: Double,
    val recordedAt: Long
)
