package com.spendly.app.domain.usecase

import com.spendly.app.domain.model.CalendarExportSourceType
import com.spendly.app.domain.repository.CalendarExportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExportedSourceIdsUseCase @Inject constructor(private val repository: CalendarExportRepository) {
    operator fun invoke(userId: String, sourceType: CalendarExportSourceType): Flow<Set<String>> =
        repository.observeExportedSourceIds(userId, sourceType)
}

class MarkCalendarExportedUseCase @Inject constructor(private val repository: CalendarExportRepository) {
    suspend operator fun invoke(userId: String, sourceType: CalendarExportSourceType, sourceId: String) =
        repository.markExported(userId, sourceType, sourceId)
}
