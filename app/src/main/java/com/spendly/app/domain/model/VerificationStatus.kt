package com.spendly.app.domain.model

/**
 * Every transaction carries this so the UI never conflates a confirmed
 * spend with one that hasn't actually been verified (see the V2 spec's
 * QR/UPI section) - manual entries are trusted at creation time, but
 * anything that comes from a launched payment intent may be pending.
 */
enum class VerificationStatus {
    MANUAL_ENTRY,
    OBSERVED_UNVERIFIED,
    LAUNCHED_UNKNOWN,
    LAUNCHED_SUCCESS,
    LAUNCHED_FAILED
}
