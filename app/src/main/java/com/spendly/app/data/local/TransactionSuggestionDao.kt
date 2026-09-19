package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionSuggestionDao {
    @Query("SELECT * FROM transaction_suggestions WHERE userId = :userId AND status = 'PENDING' ORDER BY notificationPostedAt DESC")
    fun observePending(userId: String): Flow<List<TransactionSuggestionEntity>>

    @Query("SELECT * FROM transaction_suggestions WHERE userId = :userId ORDER BY notificationPostedAt DESC")
    fun observeAll(userId: String): Flow<List<TransactionSuggestionEntity>>

    @Query("SELECT * FROM transaction_suggestions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionSuggestionEntity?

    @Query(
        "SELECT * FROM transaction_suggestions WHERE userId = :userId AND sourcePackage = :sourcePackage " +
            "AND status = 'PENDING' AND notificationPostedAt BETWEEN :fromMillis AND :toMillis"
    )
    suspend fun findPendingInWindow(userId: String, sourcePackage: String, fromMillis: Long, toMillis: Long): List<TransactionSuggestionEntity>

    @Insert
    suspend fun insert(entity: TransactionSuggestionEntity)

    @Update
    suspend fun update(entity: TransactionSuggestionEntity)
}
