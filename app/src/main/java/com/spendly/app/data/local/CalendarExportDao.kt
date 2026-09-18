package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarExportDao {
    @Query("SELECT * FROM calendar_exports WHERE userId = :userId AND sourceType = :sourceType")
    fun observeAll(userId: String, sourceType: String): Flow<List<CalendarExportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: CalendarExportEntity)
}
