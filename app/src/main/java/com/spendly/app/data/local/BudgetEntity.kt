package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val categoryId: String?,
    val amount: Double,
    val priority: String,
    val period: String,
    val createdAt: Long,
    val updatedAt: Long
)
