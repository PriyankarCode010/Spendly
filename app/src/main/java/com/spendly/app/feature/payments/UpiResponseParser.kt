package com.spendly.app.feature.payments

import android.app.Activity
import android.content.Intent
import com.spendly.app.domain.model.VerificationStatus

/**
 * Maps a UPI app's response back to our VerificationStatus. Per the NPCI
 * common library spec the "response" extra is a query-string with a Status
 * field, but not every UPI app implements this consistently - anything we
 * can't confidently parse as SUCCESS or FAILURE is treated as unknown/pending
 * rather than guessed at (see the V2 spec's QR payment flow: the app never
 * claims a payment succeeded that it can't verify).
 */
fun resolveVerificationStatus(resultCode: Int, data: Intent?): VerificationStatus {
    if (resultCode != Activity.RESULT_OK || data == null) return VerificationStatus.LAUNCHED_UNKNOWN

    val response = data.getStringExtra("response") ?: return VerificationStatus.LAUNCHED_UNKNOWN
    val fields = response.split("&").mapNotNull { pair ->
        val parts = pair.split("=", limit = 2)
        if (parts.size == 2) parts[0].trim().lowercase() to parts[1].trim() else null
    }.toMap()

    return when (fields["status"]?.uppercase()) {
        "SUCCESS" -> VerificationStatus.LAUNCHED_SUCCESS
        "FAILURE", "FAILED" -> VerificationStatus.LAUNCHED_FAILED
        "SUBMITTED", "PENDING" -> VerificationStatus.LAUNCHED_UNKNOWN
        else -> VerificationStatus.LAUNCHED_UNKNOWN
    }
}
