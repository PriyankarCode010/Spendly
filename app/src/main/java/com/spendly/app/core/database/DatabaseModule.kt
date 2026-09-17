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
            .build()
    }

    @Provides
    fun provideProfileDao(database: SpendlyDatabase) = database.profileDao()

    private fun generatePassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
