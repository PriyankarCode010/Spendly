package com.spendly.app.domain.engine.notification

import com.spendly.app.domain.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GooglePayNotificationParserTest {

    private val parser = GooglePayNotificationParser()

    private fun payload(title: String?, text: String?) = NotificationPayload(
        packageName = "com.google.android.apps.nbu.paisa.user",
        title = title,
        text = text,
        postedAt = 1_000_000L
    )

    @Test
    fun `parses a debit notification with amount, direction, and merchant`() {
        val parsed = parser.parse(payload("Payment successful", "You paid ₹250 to Amit Kumar"))

        assertEquals(250.0, parsed?.amount)
        assertEquals(TransactionDirection.DEBIT, parsed?.direction)
        assertEquals("Amit Kumar", parsed?.merchant)
    }

    @Test
    fun `parses a credit notification phrased as sent you`() {
        val parsed = parser.parse(payload("Money received", "Amit Kumar sent you ₹500"))

        assertEquals(500.0, parsed?.amount)
        assertEquals(TransactionDirection.CREDIT, parsed?.direction)
        assertEquals("Amit Kumar", parsed?.merchant)
    }

    @Test
    fun `parses a credit notification phrased as received from`() {
        val parsed = parser.parse(payload("Money received", "You received ₹750 from Priya Singh"))

        assertEquals(750.0, parsed?.amount)
        assertEquals(TransactionDirection.CREDIT, parsed?.direction)
        assertEquals("Priya Singh", parsed?.merchant)
    }

    @Test
    fun `extracts amount with commas and decimals`() {
        val parsed = parser.parse(payload("Payment successful", "You paid ₹1,250.50 to Landlord"))

        assertEquals(1250.50, parsed?.amount)
    }

    @Test
    fun `returns null for a failed payment notification`() {
        val parsed = parser.parse(payload("Payment failed", "Your payment of ₹250 to Amit Kumar failed"))

        assertNull(parsed)
    }

    @Test
    fun `returns null for an unrelated notification`() {
        val parsed = parser.parse(payload("New message", "You have a new message from support"))

        assertNull(parsed)
    }

    @Test
    fun `returns null for a malformed or empty notification`() {
        assertNull(parser.parse(payload(null, null)))
        assertNull(parser.parse(payload("", "")))
    }

    @Test
    fun `amount only notification with no recognizable direction keyword still yields amount with null direction`() {
        val parsed = parser.parse(payload("Transaction alert", "Amount of ₹99 noted"))

        assertEquals(99.0, parsed?.amount)
        assertNull(parsed?.direction)
    }
}
