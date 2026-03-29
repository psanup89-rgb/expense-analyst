package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenericParserTest {

    private val parser = GenericParser()

    @Test
    fun `canParse always returns true`() {
        assertTrue(parser.canParse("ANYTHING", "any body text"))
        assertTrue(parser.canParse("", ""))
    }

    @Test
    fun `parse extracts INR debit`() {
        val result = parser.parse("UNKNOWN",
            "Rs.500.00 debited from your account. Ref 12345.")
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("Unknown Bank", result.bankName)
    }

    @Test
    fun `parse extracts INR credit`() {
        val result = parser.parse("UNKNOWN",
            "INR 1000.00 credited to your account. Ref 67890.")
        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("INR", result.currencyCode)
    }

    @Test
    fun `parse extracts SAR debit`() {
        val result = parser.parse("UNKNOWN",
            "SAR 250.00 debited from your account at Extra Stores.")
        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("SAR", result.currencyCode)
    }

    @Test
    fun `parse extracts AED debit`() {
        val result = parser.parse("UNKNOWN",
            "AED 75.00 debited from your account at Carrefour.")
        assertNotNull(result)
        assertEquals(75.0, result!!.amount, 0.01)
        assertEquals("AED", result.currencyCode)
    }

    @Test
    fun `parse extracts merchant from at-pattern`() {
        val result = parser.parse("UNKNOWN",
            "SAR 95.00 debited. at: Starbucks\nBalance: SAR 1000.00")
        assertNotNull(result)
        assertEquals("Starbucks", result!!.merchant)
    }

    @Test
    fun `parse extracts card last-4`() {
        val result = parser.parse("UNKNOWN",
            "Rs.300.00 debited. Card:7573 at Noon. Balance: Rs.5000.00")
        assertNotNull(result)
        assertEquals("7573", result!!.accountLast4)
    }

    @Test
    fun `parse detects payment confirmation`() {
        val result = parser.parse("UNKNOWN",
            "Payment received for Rs.2000.00 towards your account.")
        assertNotNull(result)
        assertEquals(TransactionDirection.PAYMENT, result!!.type)
    }

    @Test
    fun `parse returns null for OTP message`() {
        assertNull(parser.parse("UNKNOWN", "Your OTP is 123456. Do not share."))
    }

    @Test
    fun `parse returns null for no amount`() {
        assertNull(parser.parse("UNKNOWN", "Your account has been debited."))
    }

    @Test
    fun `parse returns null for no transaction keyword`() {
        assertNull(parser.parse("UNKNOWN", "Rs.500.00 is the balance in your account."))
    }
}
