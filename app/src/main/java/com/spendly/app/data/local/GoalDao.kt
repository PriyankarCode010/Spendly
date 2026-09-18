package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<GoalEntity>>

    @Insert
    suspend fun insert(goal: GoalEntity)
}
