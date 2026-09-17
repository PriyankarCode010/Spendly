package com.spendly.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spendly.app.data.local.ProfileDao
import com.spendly.app.data.local.ProfileEntity

@Database(
    entities = [ProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
