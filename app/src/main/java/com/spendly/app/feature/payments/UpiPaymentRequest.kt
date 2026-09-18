package com.spendly.app.feature.payments

import android.net.Uri

data class UpiPaymentRequest(
    val payeeVpa: String,
    val payeeName: String?,
    val amount: Double?,
    val currency: String,
    val note: String?
)

/**
 * Parses a scanned UPI deep link (upi://pay?pa=...&pn=...&am=...&cu=...&tn=...).
 * Returns null for anything that isn't a recognizable UPI payment URI - the
 * app never guesses at a malformed or unrelated QR code.
 */
fun parseUpiUri(raw: String): UpiPaymentRequest? {
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() != "upi") return null

    val payeeVpa = uri.getQueryParameter("pa")?.takeIf { it.isNotBlank() } ?: return null

    return UpiPaymentRequest(
        payeeVpa = payeeVpa,
        payeeName = uri.getQueryParameter("pn"),
        amount = uri.getQueryParameter("am")?.toDoubleOrNull(),
        currency = uri.getQueryParameter("cu") ?: "INR",
        note = uri.getQueryParameter("tn")
    )
}

fun buildUpiPayIntentUri(request: UpiPaymentRequest, amount: Double): Uri =
    Uri.parse("upi://pay").buildUpon()
        .appendQueryParameter("pa", request.payeeVpa)
        .apply { request.payeeName?.let { appendQueryParameter("pn", it) } }
        .appendQueryParameter("am", amount.toString())
        .appendQueryParameter("cu", request.currency)
        .apply { request.note?.let { appendQueryParameter("tn", it) } }
        .build()
