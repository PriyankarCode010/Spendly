package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["resultingTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("status"), Index("resultingTransactionId")]
)
data class TransactionSuggestionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourcePackage: String,
    val amount: Double?,
    val direction: String?,
    val merchant: String?,
    val referenceId: String?,
    val notificationPostedAt: Long,
    val status: String,
    val createdAt: Long,
    val resultingTransactionId: String?
)
