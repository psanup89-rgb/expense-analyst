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

    // Real wordings (Oct 2026), names and numbers replaced. Both were dropped by every parser.
    private val international = "International transfer\nAmount: INR 2,556.77 (SAR 100.00)\nTo: Ravi Kumar\n" +
        "10000000000001\nCountry:India\nThrough:THUNES\nOn: 2026-10-11 22:55"
    private val incoming = "Incoming Transfer: AlRajhi Bank\nAmount: SAR 6,000.00\nFrom: RAVI K KUMA****\n" +
        "IBAN: ****1234\nat: 2026-10-11 22:53"

    @Test
    fun `international transfer is a SAR transfer to the recipient`() {
        val result = parser.parse("D360 Bank", international)
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)          // the SAR that left, not the INR
        assertEquals("SAR", result.currencyCode)
        assertEquals(TransactionDirection.TRANSFER, result.type)
        assertEquals("Ravi Kumar", result.merchant)
        assertNull(result.accountLast4)                     // the account number is the recipient's
        assertEquals("NET_BANKING", result.paymentMethodName)
    }

    @Test
    fun `incoming transfer is a transfer from the unmasked part of the sender's name`() {
        val result = parser.parse("D360 Bank", incoming)
        assertNotNull(result)
        assertEquals(6000.0, result!!.amount, 0.001)
        assertEquals(TransactionDirection.TRANSFER, result.type) // not income: often the user's own money
        assertEquals("RAVI K KUMA", result.merchant)
        assertNull(result.accountLast4)                          // the IBAN is the sender's
    }

    @Test
    fun `the registry saves both and still skips the transfer OTP`() {
        assertEquals(TransactionDirection.TRANSFER, ParserRegistry.parse("D360 Bank", international)?.type)
        assertEquals(TransactionDirection.TRANSFER, ParserRegistry.parse("D360 Bank", incoming)?.type)
        assertNull(ParserRegistry.parse("D360 Bank", "OTP: 1111\nAmount: SAR 100.00\nFor: International transfer"))
    }

    @Test
    fun `a maintenance notice is not a transaction`() {
        assertNull(
            ParserRegistry.parse(
                "D360 Bank",
                "Dear Customer,\nWe will conduct system maintenance on Friday, October 9, from 4-5:30 AM. " +
                    "Card services and transactions may be temporarily affected."
            )
        )
    }
}
