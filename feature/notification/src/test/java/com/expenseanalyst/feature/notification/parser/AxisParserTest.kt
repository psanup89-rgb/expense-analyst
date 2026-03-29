package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AxisParserTest {

    private val parser = AxisParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "AXISBK, true",
        "AXISBN, true",
        "AD-AXISBK, true",
        "HDFCBK, false",
        "SBIBNK, false"
    )
    fun `canParse recognises Axis sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "Rs.600.00 debited"))
    }

    @Test
    fun `parse extracts INR debit via UPI with merchant`() {
        val result = parser.parse("AXISBK",
            "Rs.600.00 debited from Axis Bank Acct XX9876 on 01-Jan-25. Trf to Zomato via UPI. Ref:987654321")
        assertNotNull(result)
        assertEquals(600.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("9876", result.accountLast4)
        assertEquals("Zomato", result.merchant)
    }

    @Test
    fun `parse extracts INR credit`() {
        val result = parser.parse("AXISBK",
            "Rs.2000.00 credited to Axis Bank Acct XX9876. Sender: John Doe. Ref:123456789")
        assertNotNull(result)
        assertEquals(2000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("INR", result.currencyCode)
    }

    @Test
    fun `parse extracts SAR forex card debit with merchant`() {
        val result = parser.parse("AXISBK",
            "Debited SAR 43.05 from Axis Bank Fx Card XX9665 on 26-03-2026 02:18:34 IST at Keemart. Bal: SAR 5318.50.")
        assertNotNull(result)
        assertEquals(43.05, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("9665", result.accountLast4)
        assertEquals("Keemart", result.merchant)
    }

    @Test
    fun `parse extracts UPI compact format with merchant`() {
        val result = parser.parse("AXISBK",
            "INR 4386.00 debited A/c no. XX0426 10-03-26, 02:47:02 UPI/P2M/600656614974/3FIVE8 TECHNOLOGIES Not you?")
        assertNotNull(result)
        assertEquals(4386.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("0426", result.accountLast4)
        assertEquals("3FIVE8 TECHNOLOGIES", result.merchant)
    }

    @ParameterizedTest
    @CsvSource(
        "AXISBK, Your Axis Bank OTP is 345678. Do not share.",
        "AXISBK, Your Axis Bank account statement is available."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
