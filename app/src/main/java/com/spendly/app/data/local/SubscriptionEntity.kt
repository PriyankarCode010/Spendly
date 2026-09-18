package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val frequency: String,
    val nextPaymentDate: Long,
    val categoryId: String?,
    val isActive: Boolean
)
