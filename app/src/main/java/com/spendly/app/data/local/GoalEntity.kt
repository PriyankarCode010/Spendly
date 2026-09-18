package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "goals", indices = [Index("userId")])
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val currentSaved: Double,
    val targetDate: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
