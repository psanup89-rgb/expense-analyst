package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Integration tests for [ParserRegistry] — verifies dispatch to correct parser.
 */
class ParserRegistryTest {

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(
        // HDFC
        "HDFCBK, Rs.500.00 debited from a/c XX1234 on 01-01-2025 at Swiggy. Avl bal Rs.12000.00, HDFC Bank, 500.0, DEBIT",
        // SBI
        "SBIBNK, Dear SBI Customer Rs 300.00 debited from A/c XX5678. Info: Zomato. Avl Bal Rs 4000.00, SBI, 300.0, DEBIT",
        // ICICI
        "ICICIB, ICICI Bank Acct XX1234: Rs 450.00 debited on 01-Jan-25. Info: Amazon. Avl Bal: Rs 5500.00, ICICI Bank, 450.0, DEBIT",
        // Axis
        "AXISBK, Rs.600.00 debited from Axis Bank Acct XX9876 on 01-Jan-25. Trf to Uber via UPI. Ref:987654321, Axis Bank, 600.0, DEBIT",
        // Kotak
        "KOTAKB, INR 750.00 debited from Kotak Bank A/c XX5678 on 01-Jan-25 via UPI to Netflix. Ref 111222333, Kotak Bank, 750.0, DEBIT",
        // Al Rajhi
        "AlRajhi, Purchase of SAR 250.00 was made using your card ending 1234 at Jarir Bookstore on 01/01/2025, Al Rajhi Bank, 250.0, DEBIT",
        // STC Bank
        "STCPay, SAR 150.00 has been paid from your STC Pay account to Noon. Ref: TXN123456, STC Bank, 150.0, DEBIT",
        // UPI
        "GPAY, You paid Rs 200 to Swiggy via Google Pay. UPI Ref: 123456789012, UPI, 200.0, DEBIT"
    )
    fun `registry dispatches to correct parser`(
        sender: String,
        body: String,
        expectedBankName: String,
        expectedAmount: Double,
        expectedType: String
    ) {
        val result = ParserRegistry.parse(sender, body)
        assertNotNull(result, "Expected parse result for sender=$sender")
        assertEquals(expectedAmount, result!!.amount, 0.01)
        val expectedDirection = TransactionDirection.valueOf(expectedType)
        assertEquals(expectedDirection, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "HDFCBK, Your HDFC credit card statement is ready.",
        "RANDOM, Hello world!"
    )
    fun `registry returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(ParserRegistry.parse(sender, body))
    }
}
