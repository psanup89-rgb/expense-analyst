package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeetaParserTest {

    private val parser = KeetaParser()

    @Test
    fun `canParse true for Keeta sender`() {
        assertTrue(parser.canParse("Keeta", "SAR 31.83 refunded to your payment method"))
    }

    @Test
    fun `canParse true for body-prefixed Keeta`() {
        val body = "[Keeta]The order was canceled. SAR 40.25 will return to the original way."
        assertTrue(parser.canParse("12345", body))
    }

    @Test
    fun `parse direct refund SMS classifies as CREDIT`() {
        val body = "SAR 31.83 refunded to your payment method on 17 Apr 2026 at 12:34."
        val result = parser.parse("Keeta", body)
        assertNotNull(result)
        assertEquals(31.83, result!!.amount, 0.01)
        assertEquals("SAR", result.currencyCode)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("Keeta", result.merchant)
    }

    // ── Issue #5 / #8: order-cancellation refund (worded as "canceled … will return") ──
    @Test
    fun `parse order cancellation classifies as CREDIT`() {
        val body = "[Keeta]The order (ending No: 9434) was canceled for a stock shortage. " +
            "SAR 40.25 will return to the original way in 1-14 workdays."
        val result = parser.parse("12345", body)
        assertNotNull(result)
        assertEquals(40.25, result!!.amount, 0.01)
        assertEquals("SAR", result.currencyCode)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("Keeta", result.merchant)
    }

    @Test
    fun `parse charge SMS classifies as DEBIT`() {
        val body = "SAR 31.83 charged for your Keeta order on 17 Apr 2026."
        val result = parser.parse("Keeta", body)
        assertNotNull(result)
        assertEquals(31.83, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
    }

    @Test
    fun `parse returns null when no refund or charge keyword present`() {
        assertNull(parser.parse("Keeta", "Welcome to Keeta!"))
    }
}
