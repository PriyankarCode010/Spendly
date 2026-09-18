package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND isActive = 1 ORDER BY nextPaymentDate ASC")
    fun observeAll(userId: String): Flow<List<SubscriptionEntity>>

    @Insert
    suspend fun insert(subscription: SubscriptionEntity)
}
