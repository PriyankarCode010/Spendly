package com.spendly.app.domain.repository

import com.spendly.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(userId: String): Flow<List<Category>>
    suspend fun ensureDefaultCategories(userId: String)
}
