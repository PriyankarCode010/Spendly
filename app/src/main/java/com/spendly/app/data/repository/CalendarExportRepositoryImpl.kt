package com.spendly.app.data.repository

import com.spendly.app.data.local.CalendarExportDao
import com.spendly.app.data.local.CalendarExportEntity
import com.spendly.app.domain.model.CalendarExportSourceType
import com.spendly.app.domain.repository.CalendarExportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarExportRepositoryImpl @Inject constructor(
    private val calendarExportDao: CalendarExportDao
) : CalendarExportRepository {

    override fun observeExportedSourceIds(
        userId: String,
        sourceType: CalendarExportSourceType
    ): Flow<Set<String>> =
        calendarExportDao.observeAll(userId, sourceType.name)
            .map { list -> list.map { it.sourceId }.toSet() }

    override suspend fun markExported(userId: String, sourceType: CalendarExportSourceType, sourceId: String) {
        calendarExportDao.upsert(
            CalendarExportEntity(
                id = "${sourceType.name}:$sourceId",
                userId = userId,
                sourceType = sourceType.name,
                sourceId = sourceId,
                exportedAt = System.currentTimeMillis()
            )
        )
    }
}
