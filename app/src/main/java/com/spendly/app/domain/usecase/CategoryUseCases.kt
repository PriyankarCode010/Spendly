package com.spendly.app.domain.usecase

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(private val repository: CategoryRepository) {
    operator fun invoke(userId: String): Flow<List<Category>> = repository.observeAll(userId)
}

class EnsureDefaultCategoriesUseCase @Inject constructor(private val repository: CategoryRepository) {
    suspend operator fun invoke(userId: String) = repository.ensureDefaultCategories(userId)
}
