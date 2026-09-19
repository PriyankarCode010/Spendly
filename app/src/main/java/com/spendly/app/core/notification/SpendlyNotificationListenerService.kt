package com.spendly.app.core.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.spendly.app.domain.engine.notification.NotificationParserRegistry
import com.spendly.app.domain.engine.notification.NotificationPayload
import com.spendly.app.domain.usecase.GetCurrentUserUseCase
import com.spendly.app.domain.usecase.IngestNotificationSuggestionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One-way observation only (V2 spec section 22 / Phase 7 approval): reads
 * only the title/text fields of notifications from the two supported
 * payment app packages, parses them with a pure domain-layer parser, and
 * hands the result to IngestNotificationSuggestionUseCase - which only ever
 * creates a PENDING TransactionSuggestion, never a Transaction. Raw
 * notification text/extras are never stored - only what the parser
 * extracts into ParsedSuggestion crosses into persistence.
 *
 * Requires the user to grant "Notification access" for Spendly in system
 * Settings - a special access the user can revoke at any time, not a
 * runtime permission this app can request via a dialog.
 */
@AndroidEntryPoint
class SpendlyNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var parserRegistry: NotificationParserRegistry
    @Inject lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    @Inject lateinit var ingestNotificationSuggestionUseCase: IngestNotificationSuggestionUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val parser = parserRegistry.parserFor(sbn.packageName) ?: return

        val extras = sbn.notification.extras
        val payload = NotificationPayload(
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = (extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString(),
            postedAt = sbn.postTime
        )

        val parsed = parser.parse(payload) ?: return

        serviceScope.launch {
            val userId = getCurrentUserUseCase()?.id ?: return@launch
            // Best-effort background observer - ingest errors are non-fatal, nothing to surface here.
            ingestNotificationSuggestionUseCase(userId, payload.packageName, parsed, payload.postedAt)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val TAG = "SpendlyNotifListener"
    }
}
