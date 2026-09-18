package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
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
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val frequency: String,
    val nextPaymentDate: Long,
    val categoryId: String?,
    val isActive: Boolean,
    val updatedAt: Long
)
