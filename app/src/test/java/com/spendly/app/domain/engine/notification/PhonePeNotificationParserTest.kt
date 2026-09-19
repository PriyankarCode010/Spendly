package com.spendly.app.domain.engine.notification

import com.spendly.app.domain.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhonePeNotificationParserTest {

    private val parser = PhonePeNotificationParser()

    private fun payload(title: String?, text: String?) = NotificationPayload(
        packageName = "com.phonepe.app",
        title = title,
        text = text,
        postedAt = 2_000_000L
    )

    @Test
    fun `parses a debit notification with amount, direction, and merchant`() {
        val parsed = parser.parse(payload("PhonePe", "You paid ₹120 to Cafe Coffee Day"))

        assertEquals(120.0, parsed?.amount)
        assertEquals(TransactionDirection.DEBIT, parsed?.direction)
        assertEquals("Cafe Coffee Day", parsed?.merchant)
    }

    @Test
    fun `parses a debit notification phrased as payment successful`() {
        val parsed = parser.parse(payload("PhonePe", "Payment of ₹300 to Electric Board successful"))

        assertEquals(300.0, parsed?.amount)
        assertEquals(TransactionDirection.DEBIT, parsed?.direction)
    }

    @Test
    fun `parses a credit notification`() {
        val parsed = parser.parse(payload("PhonePe", "You received ₹450 from Ravi Shah"))

        assertEquals(450.0, parsed?.amount)
        assertEquals(TransactionDirection.CREDIT, parsed?.direction)
        assertEquals("Ravi Shah", parsed?.merchant)
    }

    @Test
    fun `returns null for a failed payment notification`() {
        val parsed = parser.parse(payload("PhonePe", "Payment of ₹300 to Electric Board failed"))

        assertNull(parsed)
    }

    @Test
    fun `returns null for a declined payment notification`() {
        val parsed = parser.parse(payload("PhonePe", "Your payment was declined by the bank"))

        assertNull(parsed)
    }

    @Test
    fun `returns null for an unrelated notification`() {
        val parsed = parser.parse(payload("PhonePe", "Recharge your phone to get cashback offers"))

        assertNull(parsed)
    }

    @Test
    fun `returns null for a malformed or empty notification`() {
        assertNull(parser.parse(payload(null, null)))
        assertNull(parser.parse(payload("", "")))
    }

    @Test
    fun `extracts a reference id when present`() {
        val parsed = parser.parse(payload("PhonePe", "You paid ₹120 to Cafe Coffee Day. UPI transaction ID: 123456789012"))

        assertEquals("123456789012", parsed?.referenceId)
    }
}
