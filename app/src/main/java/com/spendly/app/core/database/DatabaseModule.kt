package com.spendly.app.core.database

import android.content.Context
import androidx.room.Room
import com.spendly.app.core.security.SecurePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.inject.Singleton

private const val DB_NAME = "spendly.db"
private const val PASSPHRASE_KEY = "db_passphrase"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securePreferences: SecurePreferences
    ): SpendlyDatabase {
        SQLiteDatabase.loadLibs(context)
        val passphrase = securePreferences.getString(PASSPHRASE_KEY) ?: generatePassphrase().also {
            securePreferences.putString(PASSPHRASE_KEY, it)
        }
        val factory = SupportFactory(passphrase.toByteArray(Charsets.UTF_8))

        return Room.databaseBuilder(context, SpendlyDatabase::class.java, DB_NAME)
            .openHelperFactory(factory)
            // Pre-release: no production data exists yet, so a destructive
            // migration is fine until the schema stabilizes.
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideProfileDao(database: SpendlyDatabase) = database.profileDao()

    @Provides
    fun provideCategoryDao(database: SpendlyDatabase) = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: SpendlyDatabase) = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: SpendlyDatabase) = database.budgetDao()

    @Provides
    fun provideSubscriptionDao(database: SpendlyDatabase) = database.subscriptionDao()

    @Provides
    fun provideGoalDao(database: SpendlyDatabase) = database.goalDao()

    @Provides
    fun provideBmiDao(database: SpendlyDatabase) = database.bmiDao()

    @Provides
    fun provideCalendarExportDao(database: SpendlyDatabase) = database.calendarExportDao()

    @Provides
    fun provideTransactionSuggestionDao(database: SpendlyDatabase) = database.transactionSuggestionDao()

    private fun generatePassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
