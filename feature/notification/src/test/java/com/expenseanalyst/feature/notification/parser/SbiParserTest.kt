package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SbiParserTest {

    private val parser = SbiParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "SBIBNK, true",
        "AD-SBIBNK, true",
        "SBIPSG, true",
        "SBIINB, true",
        "HDFCBK, false",
        "ICICIB, false"
    )
    fun `canParse recognises SBI sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "Rs 500.00 debited"))
    }

    @ParameterizedTest(name = "debit: {1}")
    @CsvSource(
        "SBIBNK, Dear SBI Customer Rs 500.00 debited from A/c No. XXXXXXXX1234 on 01-01-25. Info: Swiggy Food. Avl. Bal: Rs 8000.00, 500.0, 1234, Swiggy Food",
        "SBIBNK, Rs 1500.00 debited from A/c XXXXXXXX5678 on 01-01-25. Avl. Bal: Rs 4000.00, 1500.0, 5678, null"
    )
    fun `parse extracts debit transactions`(
        sender: String, body: String,
        expectedAmount: Double, expectedAccount: String, expectedMerchant: String
    ) {
        val result = parser.parse(sender, body)
        assertNotNull(result)
        assertEquals(expectedAmount, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals(expectedAccount, result.accountLast4)
        if (expectedMerchant != "null") assertEquals(expectedMerchant, result.merchant)
    }

    @Test
    fun `parse extracts credit transaction`() {
        val result = parser.parse("SBIBNK",
            "Dear Customer Rs 2000.00 credited to your A/c XXXXXXXX5678 on 01-01-25. Ref No 1234567890.")
        assertNotNull(result)
        assertEquals(2000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("5678", result.accountLast4)
    }

    @Test
    fun `parse extracts credit card payment`() {
        val result = parser.parse("SBIBNK",
            "We have received payment of Rs.6544.00 via BBPS & the same has been credited to your SBI Credit Card.")
        assertNotNull(result)
        assertEquals(6544.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.PAYMENT, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "SBIBNK, Your OTP is 123456. Do not share.",
        "SBIBNK, Your SBI account statement for March is ready."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
