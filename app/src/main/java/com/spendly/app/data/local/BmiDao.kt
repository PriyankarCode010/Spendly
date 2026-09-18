package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BmiDao {
    @Query("SELECT * FROM bmi_entries WHERE userId = :userId ORDER BY recordedAt DESC")
    fun observeAll(userId: String): Flow<List<BmiEntity>>

    @Insert
    suspend fun insert(entry: BmiEntity)
}
