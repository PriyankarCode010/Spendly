package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val amount: Double,
    val categoryId: String?,
    val merchant: String,
    val description: String,
    val transactionDate: Long,
    val verificationStatus: String,
    val createdAt: Long,
    val updatedAt: Long
)
