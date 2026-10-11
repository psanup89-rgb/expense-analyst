package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class IciciParserTest {

    private val parser = IciciParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "ICICIB, true",
        "ICICIBANK, true",
        "AD-ICICIB, true",
        "ICICIT, true",
        "HDFCBK, false",
        "SBIBNK, false"
    )
    fun `canParse recognises ICICI sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "Rs 450.00 debited"))
    }

    @Test
    fun `parse extracts debit with account and merchant`() {
        val result = parser.parse("ICICIB",
            "ICICI Bank Acct XX1234: Rs 450.00 debited on 01-Jan-25. Info: UPI/123456/Zomato. Avl Bal: Rs 5500.00")
        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("1234", result.accountLast4)
    }

    @Test
    fun `parse extracts credit with account`() {
        val result = parser.parse("ICICIB",
            "ICICI Bank Acct XX1234: Rs 1500.00 credited on 01-Jan-25. Ref 98765432.")
        assertNotNull(result)
        assertEquals(1500.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("1234", result.accountLast4)
    }

    @Test
    fun `parse extracts credit card payment`() {
        val result = parser.parse("ICICIBANK",
            "Payment of Rs 4615.92 has been received on your ICICI Bank Credit Card XX9008 through Bharat Bill Payment System")
        assertNotNull(result)
        assertEquals(4615.92, result!!.amount, 0.01)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("9008", result.accountLast4)
    }

    @Test
    fun `parse extracts amount with commas`() {
        val result = parser.parse("ICICIB",
            "ICICI Bank Acct XX5678: Rs 12500.00 debited on 01-Jan-25.")
        assertNotNull(result)
        assertEquals(12500.0, result!!.amount, 0.01)
    }

    // Real wording (Oct 2026), digits and phone numbers replaced. Was dropped by every parser.
    private val cardSpend = "Rs 1,22,099.00 spent on ICICI Bank Card XX1234 on 11-Oct-26 at WWWCROMACOM. " +
        "Avl Lmt: Rs 27,901.00. To dispute, call 18000000000/SMS BLOCK 1234 to 9000000000. " +
        "To convert this txn to EMI give a missed call on 9000000001. Know more about EMI conversion at https://example.com/x"

    @Test
    fun `parse reads a credit card purchase worded as spent`() {
        val result = parser.parse("ICICIT", cardSpend)
        assertNotNull(result)
        assertEquals(122099.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("1234", result.accountLast4)
        assertEquals("CROMA", result.merchant)
        assertEquals("CREDIT_CARD", result.paymentMethodName)
    }

    @Test
    fun `a spend at a plain merchant keeps its name`() {
        val result = parser.parse(
            "ICICIT",
            "Rs 450.00 spent on ICICI Bank Card XX1234 on 02-Oct-26 at SWIGGY. Avl Lmt: Rs 50,000.00."
        )
        assertEquals("SWIGGY", result?.merchant)
        assertEquals(TransactionDirection.DEBIT, result?.type)
    }

    @Test
    fun `the registry saves the card purchase instead of dropping it`() {
        val result = ParserRegistry.parse("ICICIT", cardSpend)
        assertNotNull(result)
        assertEquals(122099.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("CROMA", result.merchant)
    }

    @ParameterizedTest
    @CsvSource(
        "ICICIB, Your OTP for ICICI is 7890. Valid for 10 mins.",
        "ICICIB, Your ICICI Bank account statement is ready for download."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
