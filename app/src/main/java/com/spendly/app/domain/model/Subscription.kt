package com.spendly.app.domain.model

data class Subscription(
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val frequency: SubscriptionFrequency,
    val nextPaymentDate: Long,
    val categoryId: String?,
    val isActive: Boolean,
    val updatedAt: Long
)
