package com.spendly.app.core.notification

import com.spendly.app.domain.engine.notification.GooglePayNotificationParser
import com.spendly.app.domain.engine.notification.NotificationParserRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.spendly.app.domain.engine.notification.PhonePeNotificationParser
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationParserModule {

    @Provides
    @Singleton
    fun provideNotificationParserRegistry(): NotificationParserRegistry =
        NotificationParserRegistry(
            listOf(
                GooglePayNotificationParser(),
                PhonePeNotificationParser()
            )
        )
}
