package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class HdfcParserTest {

    private val parser = HdfcParser()

    @ParameterizedTest(name = "canParse sender={0}")
    @CsvSource(
        "HDFCBK, true",
        "AD-HDFCBK, true",
        "BK-HDFCBK, true",
        "SBIBNK, false",
        "ICICI, false"
    )
    fun `canParse recognises HDFC sender IDs`(sender: String, expected: Boolean) {
        val result = parser.canParse(sender, "Rs.500.00 debited from a/c XX1234")
        assertEquals(expected, result)
    }

    @ParameterizedTest(name = "debit amount={1}")
    @CsvSource(
        "HDFCBK, Rs.500.00 debited from a/c XX1234 on 01-01-2025 at Swiggy. Avl bal Rs.12000.00, 500.0, DEBIT, Swiggy, 1234",
        "HDFCBK, Rs.1000 debited from a/c XX5678 on 01-01-2025. Avl bal Rs.8000.00, 1000.0, DEBIT, null, 5678",
        "HDFCBK, INR 250.00 sent via UPI to Zomato. UPI Ref:123456789012. Avl Bal:INR 9800.00, 250.0, DEBIT, null, null"
    )
    fun `parse extracts debit transactions correctly`(
        sender: String,
        body: String,
        expectedAmount: Double,
        expectedType: String,
        expectedMerchant: String,
        expectedAccount: String
    ) {
        val result = parser.parse(sender, body)
        assertNotNull(result)
        assertEquals(expectedAmount, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        if (expectedAccount != "null") assertEquals(expectedAccount, result.accountLast4)
    }

    @ParameterizedTest(name = "credit")
    @CsvSource(
        "HDFCBK, Rs.1000.00 credited to a/c XX1234 on 01-01-2025. Ref no 12345678, 1000.0, CREDIT"
    )
    fun `parse extracts credit transactions correctly`(
        sender: String,
        body: String,
        expectedAmount: Double,
        expectedType: String
    ) {
        val result = parser.parse(sender, body)
        assertNotNull(result)
        assertEquals(expectedAmount, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "HDFCBK, Your OTP is 123456. Do not share.",
        "HDFCBK, Your HDFC account statement is ready."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        val result = parser.parse(sender, body)
        assertNull(result)
    }
}
