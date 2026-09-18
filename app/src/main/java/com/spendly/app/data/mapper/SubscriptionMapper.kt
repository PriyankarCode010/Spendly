package com.spendly.app.data.mapper

import com.spendly.app.data.local.SubscriptionEntity
import com.spendly.app.domain.model.Subscription
import com.spendly.app.domain.model.SubscriptionFrequency

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    id = id,
    userId = userId,
    name = name,
    amount = amount,
    frequency = SubscriptionFrequency.valueOf(frequency),
    nextPaymentDate = nextPaymentDate,
    categoryId = categoryId,
    isActive = isActive,
    updatedAt = updatedAt
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    userId = userId,
    name = name,
    amount = amount,
    frequency = frequency.name,
    nextPaymentDate = nextPaymentDate,
    categoryId = categoryId,
    isActive = isActive,
    updatedAt = updatedAt
)
