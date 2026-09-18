package com.spendly.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_exports", indices = [Index("userId", "sourceType")])
data class CalendarExportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceType: String,
    val sourceId: String,
    val exportedAt: Long
)
