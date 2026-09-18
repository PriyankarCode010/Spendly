package com.spendly.app.data.mapper

import com.spendly.app.data.local.BmiEntity
import com.spendly.app.domain.model.BmiEntry

fun BmiEntity.toDomain(): BmiEntry = BmiEntry(
    id = id,
    userId = userId,
    heightCm = heightCm,
    weightKg = weightKg,
    recordedAt = recordedAt
)

fun BmiEntry.toEntity(): BmiEntity = BmiEntity(
    id = id,
    userId = userId,
    heightCm = heightCm,
    weightKg = weightKg,
    recordedAt = recordedAt
)
