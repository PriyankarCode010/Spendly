package com.spendly.app.domain.repository

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.BmiEntry
import kotlinx.coroutines.flow.Flow

interface BmiRepository {
    fun observeAll(userId: String): Flow<List<BmiEntry>>
    suspend fun addEntry(entry: BmiEntry): AppResult<Unit>
}
