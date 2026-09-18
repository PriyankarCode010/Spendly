package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun observeAll(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId")
    suspend fun countForUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}
