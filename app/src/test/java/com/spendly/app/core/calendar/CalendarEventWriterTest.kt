package com.spendly.app.core.calendar

import android.content.Intent
import android.provider.CalendarContract
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarEventWriterTest {

    @Test
    fun `buildCalendarInsertIntent targets the events insert action with no calendar permission needed`() {
        val intent = buildCalendarInsertIntent(
            title = "Netflix payment due",
            description = "Spendly reminder",
            beginMillis = 1_000_000L,
            endMillis = 2_000_000L
        )

        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, intent.data)
        assertEquals("Netflix payment due", intent.getStringExtra(CalendarContract.Events.TITLE))
        assertEquals("Spendly reminder", intent.getStringExtra(CalendarContract.Events.DESCRIPTION))
        assertEquals(1_000_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1L))
        assertEquals(2_000_000L, intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1L))
    }
}
