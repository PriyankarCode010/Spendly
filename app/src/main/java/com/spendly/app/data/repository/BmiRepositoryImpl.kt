package com.spendly.app.data.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.data.local.BmiDao
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.model.BmiEntry
import com.spendly.app.domain.repository.BmiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BmiRepositoryImpl @Inject constructor(
    private val bmiDao: BmiDao
) : BmiRepository {

    override fun observeAll(userId: String): Flow<List<BmiEntry>> =
        bmiDao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun addEntry(entry: BmiEntry): AppResult<Unit> = runCatching {
        bmiDao.insert(entry.toEntity())
    }.fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Error(it.message ?: "Failed to save BMI entry", it) }
    )
}
