package com.spendly.app.core.calendar

import android.content.Intent
import android.provider.CalendarContract

/**
 * One-way export only: Spendly -> Device Calendar. Delegates to the user's
 * own calendar app via ACTION_INSERT (same hand-off pattern as the UPI
 * payment flow) instead of writing to the Calendar Provider directly - this
 * needs zero calendar permissions and never queries or reads any existing
 * calendar or event. The user reviews and saves the event themselves inside
 * their calendar app; Spendly never confirms the save happened.
 */
fun buildCalendarInsertIntent(
    title: String,
    description: String,
    beginMillis: Long,
    endMillis: Long
): Intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
    putExtra(CalendarContract.Events.TITLE, title)
    putExtra(CalendarContract.Events.DESCRIPTION, description)
    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
}
