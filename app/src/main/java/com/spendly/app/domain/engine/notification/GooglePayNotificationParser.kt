package com.spendly.app.domain.engine.notification

import com.spendly.app.domain.model.TransactionDirection

/**
 * Best-effort parser for Google Pay notification text. Regex patterns are
 * calibrated on commonly documented GPay notification phrasing, not
 * verified against live device notifications - expect tuning once
 * field-tested against real notifications across GPay versions/locales.
 */
class GooglePayNotificationParser : NotificationParser {

    override val packageName: String = "com.google.android.apps.nbu.paisa.user"

    override fun parse(payload: NotificationPayload): ParsedSuggestion? {
        val combined = listOfNotNull(payload.title, payload.text).joinToString(" ").trim()
        if (combined.isEmpty()) return null
        if (NotificationParsingUtils.isFailureNotification(combined)) return null

        // The actionable sentence ("X paid Y to Z") lives in the body text on
        // GPay, not the title - using text alone (falling back to title only
        // if text is missing) avoids the title's boilerplate words leaking
        // into a "name before phrase" merchant match.
        val content = payload.text?.takeIf { it.isNotBlank() } ?: payload.title ?: ""
        val amount = NotificationParsingUtils.extractAmount(combined)
        val referenceId = NotificationParsingUtils.extractReferenceId(combined)

        val lower = content.lowercase()
        return when {
            lower.contains("sent you") -> ParsedSuggestion(
                amount = amount,
                direction = TransactionDirection.CREDIT,
                merchant = NotificationParsingUtils.extractNameBeforePhrase(content, "sent you"),
                referenceId = referenceId
            )
            lower.contains("you received") || lower.contains("received from") -> ParsedSuggestion(
                amount = amount,
                direction = TransactionDirection.CREDIT,
                merchant = NotificationParsingUtils.extractMerchantAfterFrom(content),
                referenceId = referenceId
            )
            lower.contains("you paid") || lower.contains("paid to") || lower.contains("you sent") -> ParsedSuggestion(
                amount = amount,
                direction = TransactionDirection.DEBIT,
                merchant = NotificationParsingUtils.extractMerchantAfterTo(content),
                referenceId = referenceId
            )
            amount != null -> ParsedSuggestion(amount = amount, direction = null, merchant = null, referenceId = referenceId)
            else -> null
        }
    }
}
