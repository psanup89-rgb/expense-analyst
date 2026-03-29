package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AlinmaParserTest {

    private val parser = AlinmaParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "Alinma, true",
        "ALINMA, true",
        "alinma, true",
        "AlRajhi, false",
        "HDFCBK, false"
    )
    fun `canParse recognises Alinma sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "SAR 180.00 used"))
    }

    @Test
    fun `parse extracts POS debit with card and merchant`() {
        val result = parser.parse("Alinma",
            "Your Alinma card ending 4321 has been used for SAR 180.00 at Extra Stores on 01/01/2025. Available balance: SAR 4200.00")
        assertNotNull(result)
        assertEquals(180.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("4321", result.accountLast4)
        assertEquals("Extra Stores", result.merchant)
    }

    @Test
    fun `parse extracts credit with account`() {
        val result = parser.parse("ALINMA",
            "SAR 5000.00 has been credited to your Alinma account ending 4321. Ref: 112233445")
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("4321", result.accountLast4)
        assertEquals("112233445", result.referenceNumber)
    }

    @Test
    fun `parse extracts purchase transaction`() {
        val result = parser.parse("Alinma",
            "SAR 95.00 purchase at Carrefour using your Alinma card 1234 on 15-Mar-25.")
        assertNotNull(result)
        assertEquals(95.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "Alinma, Your Alinma OTP is 223344.",
        "ALINMA, Your Alinma account has been verified."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
