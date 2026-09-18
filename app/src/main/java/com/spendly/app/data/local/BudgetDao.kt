package com.spendly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Query(
        "SELECT * FROM budgets WHERE userId = :userId AND period = :period AND id != :excludeId AND " +
            "((categoryId IS NULL AND :categoryId IS NULL) OR categoryId = :categoryId) LIMIT 1"
    )
    suspend fun findDuplicate(userId: String, categoryId: String?, period: String, excludeId: String): BudgetEntity?

    @Insert
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)
}
