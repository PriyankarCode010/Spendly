package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :userId LIMIT 1")
    fun observe(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :userId LIMIT 1")
    suspend fun get(userId: String): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profiles")
    suspend fun clear()
}
