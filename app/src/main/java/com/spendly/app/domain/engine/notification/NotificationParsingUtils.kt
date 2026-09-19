package com.spendly.app.domain.engine.notification

/**
 * Shared best-effort regex helpers for payment-app notification text.
 * Calibrated on commonly documented GPay/PhonePe notification phrasing, not
 * verified against live device notifications - real-world text should be
 * used to tune these patterns further once the feature is field-tested.
 */
internal object NotificationParsingUtils {

    private val AMOUNT_REGEX = Regex("""₹\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val TO_MERCHANT_REGEX = Regex("""\bto\s+([A-Za-z0-9][A-Za-z0-9 .&'-]*?)(?=\s+(?:using|via|from)\b|[.,]|$)""", RegexOption.IGNORE_CASE)
    private val FROM_MERCHANT_REGEX = Regex("""\bfrom\s+([A-Za-z0-9][A-Za-z0-9 .&'-]*?)(?=\s+(?:using|via|to)\b|[.,]|$)""", RegexOption.IGNORE_CASE)
    private val REFERENCE_ID_REGEX = Regex(
        """(?:UPI\s*(?:txn|transaction)?\s*(?:id|ID)|Ref(?:erence)?\s*(?:No\.?|ID)?)\s*[:#]?\s*([A-Za-z0-9]{6,})""",
        RegexOption.IGNORE_CASE
    )
    private val FAILURE_KEYWORDS = listOf("failed", "declined", "unsuccessful", "not successful", "could not be completed")

    fun isFailureNotification(text: String): Boolean {
        val lower = text.lowercase()
        return FAILURE_KEYWORDS.any { lower.contains(it) }
    }

    fun extractAmount(text: String): Double? =
        AMOUNT_REGEX.find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    fun extractMerchantAfterTo(text: String): String? =
        TO_MERCHANT_REGEX.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    fun extractMerchantAfterFrom(text: String): String? =
        FROM_MERCHANT_REGEX.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    fun extractReferenceId(text: String): String? =
        REFERENCE_ID_REGEX.find(text)?.groupValues?.get(1)

    fun extractNameBeforePhrase(text: String, phrase: String): String? {
        val regex = Regex("""^(.*?)\s+${Regex.escape(phrase)}""", RegexOption.IGNORE_CASE)
        return regex.find(text.trim())?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
