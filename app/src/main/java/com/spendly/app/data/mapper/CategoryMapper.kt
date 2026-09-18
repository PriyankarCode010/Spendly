package com.spendly.app.data.mapper

import com.spendly.app.data.local.CategoryEntity
import com.spendly.app.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    userId = userId,
    name = name,
    isDefault = isDefault
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    isDefault = isDefault
)
