package com.spendly.app.domain.engine.notification

import com.spendly.app.domain.model.TransactionDirection

/**
 * Best-effort parser for PhonePe notification text. Regex patterns are
 * calibrated on commonly documented PhonePe notification phrasing, not
 * verified against live device notifications - expect tuning once
 * field-tested against real notifications across PhonePe versions/locales.
 */
class PhonePeNotificationParser : NotificationParser {

    override val packageName: String = "com.phonepe.app"

    override fun parse(payload: NotificationPayload): ParsedSuggestion? {
        val combined = listOfNotNull(payload.title, payload.text).joinToString(" ").trim()
        if (combined.isEmpty()) return null
        if (NotificationParsingUtils.isFailureNotification(combined)) return null

        // The actionable sentence lives in the body text on PhonePe, not the
        // title ("PhonePe") - using text alone avoids title boilerplate
        // leaking into merchant extraction.
        val content = payload.text?.takeIf { it.isNotBlank() } ?: payload.title ?: ""
        val amount = NotificationParsingUtils.extractAmount(combined)
        val referenceId = NotificationParsingUtils.extractReferenceId(combined)

        val lower = content.lowercase()
        return when {
            lower.contains("you received") || lower.contains("received from") -> ParsedSuggestion(
                amount = amount,
                direction = TransactionDirection.CREDIT,
                merchant = NotificationParsingUtils.extractMerchantAfterFrom(content),
                referenceId = referenceId
            )
            lower.contains("you paid") || lower.contains("paid to") || lower.contains("payment") && lower.contains("successful") -> ParsedSuggestion(
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
