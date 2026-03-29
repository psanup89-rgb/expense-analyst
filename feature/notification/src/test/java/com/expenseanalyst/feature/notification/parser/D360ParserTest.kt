package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class D360ParserTest {

    private val parser = D360Parser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "D360, true",
        "D360Bank, true",
        "d360, true",
        "AlRajhi, false",
        "HDFCBK, false"
    )
    fun `canParse recognises D360 sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "SAR 95.00 paid"))
    }

    @Test
    fun `parse extracts SAR payment with transaction ref`() {
        val result = parser.parse("D360",
            "D360 Bank: SAR 95.00 paid Transaction ID: 9988776655 Balance: SAR 1200.00")
        assertNotNull(result)
        assertEquals(95.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("9988776655", result.referenceNumber)
    }

    @Test
    fun `parse extracts SAR received`() {
        val result = parser.parse("D360Bank",
            "D360 Bank: SAR 250.00 received. Transaction ID: 1122334455.")
        assertNotNull(result)
        assertEquals(250.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("1122334455", result.referenceNumber)
    }

    @Test
    fun `parse extracts debited amount`() {
        val result = parser.parse("D360",
            "D360 Bank: SAR 320.00 debited. Txn ID: 5544332211.")
        assertNotNull(result)
        assertEquals(320.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "D360, Your D360 OTP is 556677.",
        "D360Bank, Welcome to D360 Bank."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
