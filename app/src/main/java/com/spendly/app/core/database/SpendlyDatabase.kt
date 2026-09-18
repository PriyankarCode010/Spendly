package com.spendly.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spendly.app.data.local.BudgetDao
import com.spendly.app.data.local.BudgetEntity
import com.spendly.app.data.local.CategoryDao
import com.spendly.app.data.local.CategoryEntity
import com.spendly.app.data.local.GoalDao
import com.spendly.app.data.local.GoalEntity
import com.spendly.app.data.local.ProfileDao
import com.spendly.app.data.local.ProfileEntity
import com.spendly.app.data.local.SubscriptionDao
import com.spendly.app.data.local.SubscriptionEntity
import com.spendly.app.data.local.TransactionDao
import com.spendly.app.data.local.TransactionEntity

@Database(
    entities = [
        ProfileEntity::class, CategoryEntity::class, TransactionEntity::class,
        BudgetEntity::class, SubscriptionEntity::class, GoalEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun goalDao(): GoalDao
}
