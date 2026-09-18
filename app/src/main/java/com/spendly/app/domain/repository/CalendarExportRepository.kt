package com.spendly.app.domain.repository

import com.spendly.app.domain.model.CalendarExportSourceType
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight dedup record only - Spendly never queries or reads the
 * device's actual calendar (one-way export: Spendly -> Device Calendar).
 * This just remembers which local Subscription/Goal rows the user already
 * chose to export, so the UI can label the action "Re-add" instead of
 * silently creating a second calendar entry on every tap.
 */
interface CalendarExportRepository {
    fun observeExportedSourceIds(userId: String, sourceType: CalendarExportSourceType): Flow<Set<String>>
    suspend fun markExported(userId: String, sourceType: CalendarExportSourceType, sourceId: String)
}
