package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class KotakParserTest {

    private val parser = KotakParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "KOTAKB, true",
        "KOTAK, true",
        "AD-KOTAK, true",
        "HDFCBK, false",
        "SBIBNK, false"
    )
    fun `canParse recognises Kotak sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "INR 750.00 debited"))
    }

    @Test
    fun `parse extracts debit with account and merchant`() {
        val result = parser.parse("KOTAKB",
            "INR 750.00 debited from Kotak Bank A/c XX5678 on 01-Jan-25 via UPI to Uber. Ref 111222333")
        assertNotNull(result)
        assertEquals(750.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("5678", result.accountLast4)
    }

    @Test
    fun `parse extracts credit with account`() {
        val result = parser.parse("KOTAK",
            "INR 3000.00 credited to your Kotak A/c XX5678 on 01-Jan-25. Ref 444555666")
        assertNotNull(result)
        assertEquals(3000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("5678", result.accountLast4)
        assertEquals("444555666", result.referenceNumber)
    }

    @Test
    fun `parse handles amount with commas`() {
        val result = parser.parse("KOTAKB",
            "INR 15000.00 debited from Kotak Bank A/c XX1234 on 15-Mar-25. Ref 999888777")
        assertNotNull(result)
        assertEquals(15000.0, result!!.amount, 0.01)
    }

    @ParameterizedTest
    @CsvSource(
        "KOTAKB, Your Kotak OTP is 654321.",
        "KOTAK, Your Kotak Bank statement is ready."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
