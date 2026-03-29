package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MubasherParserTest {

    private val parser = MubasherParser()

    @Test
    fun `canParse true for Mubasher sender`() {
        assertTrue(parser.canParse("Mubasher", "Amount:SAR 240"))
    }

    @Test
    fun `canParse true for body fingerprint with Bills Payment`() {
        val body = "Reason:Bills Payment - Mubasher App\nBill Payment\nFrom:6805\nAmount:SAR 240\nService:ENBD PAYMENTS"
        assertTrue(parser.canParse("74100", body))
    }

    @Test
    fun `canParse true for body fingerprint with Amount SAR`() {
        assertTrue(parser.canParse("99999", "Amount:SAR 500\nBiller:125"))
    }

    @Test
    fun `canParse false for unrelated sender and body`() {
        val result = parser.canParse("HDFCBK", "Rs 500.00 debited from A/c XX1234")
        assertEquals(false, result)
    }

    @Test
    fun `parse extracts bill payment from Mubasher sample`() {
        val body = """
            Reason:Bills Payment - Mubasher App
            Bill Payment
            From:6805
            Amount:SAR 240
            Biller:125
            Service:ENBD PAYMENTS
            Bill:01600000025919
            26/3/28 22:10
        """.trimIndent()
        val result = parser.parse("74100", body)
        assertNotNull(result)
        assertEquals(240.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("6805", result.accountLast4)
        assertEquals("ENBD PAYMENTS", result.merchant)
        assertEquals("01600000025919", result.referenceNumber)
        assertEquals("Mubasher", result.bankName)
    }

    @Test
    fun `parse extracts decimal amount`() {
        val body = """
            Reason:Bills Payment - Mubasher App
            Bill Payment
            From:1234
            Amount:SAR 1500.75
            Service:STCPAY PAYMENTS
            Bill:REF123456
        """.trimIndent()
        val result = parser.parse("Mubasher", body)
        assertNotNull(result)
        assertEquals(1500.75, result!!.amount, 0.01)
    }

    @Test
    fun `parse returns null when no Amount SAR`() {
        val body = "Reason:Bills Payment - Mubasher App\nFrom:6805\nService:TEST"
        assertNull(parser.parse("Mubasher", body))
    }
}
