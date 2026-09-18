package com.spendly.app.domain.usecase

import com.spendly.app.core.common.AppResult
import com.spendly.app.domain.model.BmiEntry
import com.spendly.app.domain.repository.BmiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBmiHistoryUseCase @Inject constructor(private val repository: BmiRepository) {
    operator fun invoke(userId: String): Flow<List<BmiEntry>> = repository.observeAll(userId)
}

class AddBmiEntryUseCase @Inject constructor(private val repository: BmiRepository) {
    suspend operator fun invoke(entry: BmiEntry): AppResult<Unit> = repository.addEntry(entry)
}
