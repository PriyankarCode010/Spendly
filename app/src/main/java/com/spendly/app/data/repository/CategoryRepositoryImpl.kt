package com.spendly.app.data.repository

import com.spendly.app.data.local.CategoryDao
import com.spendly.app.data.local.CategoryEntity
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

val DEFAULT_CATEGORY_NAMES = listOf(
    "Food", "Transport", "Shopping", "Housing", "Bills", "Utilities",
    "Entertainment", "Subscriptions", "Healthcare", "Education", "Travel",
    "Personal", "Investment", "Salary", "Freelancing", "Other"
)

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeAll(userId: String): Flow<List<Category>> =
        categoryDao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun ensureDefaultCategories(userId: String) {
        if (categoryDao.countForUser(userId) > 0) return
        val defaults = DEFAULT_CATEGORY_NAMES.map { name ->
            CategoryEntity(id = UUID.randomUUID().toString(), userId = userId, name = name, isDefault = true)
        }
        categoryDao.insertAll(defaults)
    }
}
