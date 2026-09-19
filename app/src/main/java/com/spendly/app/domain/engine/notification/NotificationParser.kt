package com.spendly.app.domain.engine.notification

import com.spendly.app.domain.model.TransactionDirection

/**
 * Platform-agnostic view of a posted notification - deliberately not
 * android.service.notification.StatusBarNotification, so parsers stay in
 * the pure, testable domain layer with no Android framework dependency.
 * Only the fields a parser might need are carried; raw notification objects
 * (with OTPs, unrelated fields, etc.) never reach this layer.
 */
data class NotificationPayload(
    val packageName: String,
    val title: String?,
    val text: String?,
    val postedAt: Long
)

/**
 * Best-effort extraction only. Null return from parse() means "not a
 * transaction notification" (unrelated, malformed, or a failure/decline
 * notification - failures are deliberately never turned into a suggestion,
 * since there is nothing to confirm).
 */
data class ParsedSuggestion(
    val amount: Double?,
    val direction: TransactionDirection?,
    val merchant: String?,
    val referenceId: String?
)

interface NotificationParser {
    val packageName: String
    fun parse(payload: NotificationPayload): ParsedSuggestion?
}

/**
 * Extensible registry: adding a new payment app's parser (Paytm, BHIM, etc.)
 * is a new NotificationParser implementation registered here, with no
 * changes required to the listener service or suggestion pipeline.
 */
class NotificationParserRegistry(private val parsers: List<NotificationParser>) {
    fun parserFor(packageName: String): NotificationParser? =
        parsers.find { it.packageName == packageName }
}
