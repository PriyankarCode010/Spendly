package com.spendly.app.domain.model

enum class SubscriptionFrequency(val perYear: Double) {
    WEEKLY(52.0),
    MONTHLY(12.0),
    QUARTERLY(4.0),
    YEARLY(1.0)
}
