package com.spendly.app.data.mapper

import com.spendly.app.data.local.ProfileEntity
import com.spendly.app.domain.model.Profile

fun ProfileEntity.toDomain(): Profile = Profile(
    userId = id,
    currency = currency,
    monthlyIncome = monthlyIncome,
    currentBalance = currentBalance,
    updatedAt = updatedAt
)

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    id = userId,
    currency = currency,
    monthlyIncome = monthlyIncome,
    currentBalance = currentBalance,
    updatedAt = updatedAt
)
