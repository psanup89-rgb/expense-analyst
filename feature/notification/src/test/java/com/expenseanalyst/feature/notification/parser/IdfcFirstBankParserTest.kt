package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class IdfcFirstBankParserTest {

    private val parser = IdfcFirstBankParser()

    @ParameterizedTest(name = "canParse sender={0}")
    @CsvSource(
        "JK-IDFCFB, true",
        "AX-IDFCFB-S, true",
        "TMIDFCFB, true",
        "JKIDFCFB, true",
        "VM-IDFCFB-S, true",
        "JM-IDFCFB-T, true",
        "AlRajhi, false",
        "HDFCBK, false",
        "EmiratesNBD, false"
    )
    fun `canParse recognises IDFC FIRST Bank sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "some body"))
    }

    @Test
    fun `parse credit card spend - Transaction Successful`() {
        val body = "Transaction Successful! INR 392.43 spent on your IDFC FIRST Bank Credit Card " +
            "ending XX6426 at ZOMATO on 24 MAR 2026 at 07:38 PM Avbl Limit: INR 166566.96 " +
            "If not done by you, call 180010888 for dispute or to block your card SMS CCBLOCK 6426 to 5676732"

        val result = parser.parse("AX-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(392.43, result!!.amount, 0.01)
        assertEquals("INR", result.currencyCode)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("ZOMATO", result.merchant)
        assertEquals("6426", result.accountLast4)
        assertEquals("IDFC First Bank", result.bankName)
    }

    @Test
    fun `parse credit card spend - Delicious Purchase prefix`() {
        val body = "Delicious Purchase! INR 399.93 spent on your IDFC FIRST Bank Credit Card " +
            "ending XX6426 at ZOMATO LIMITED on 24 MAR 2026 at 07:36 PM Avbl Limit: INR 166959.39 " +
            "If not done by you, call 180010888"

        val result = parser.parse("AX-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(399.93, result!!.amount, 0.01)
        assertEquals("ZOMATO LIMITED", result.merchant)
        assertEquals("6426", result.accountLast4)
    }

    @Test
    fun `parse credit card spend - Safe Travels prefix`() {
        val body = "Safe Travels! INR 14598.00 spent on your IDFC FIRST Bank Credit Card " +
            "ending XX6887 at CLEARTRIP PRIVATE LIMI on 16 FEB 2026 at 10:31 AM Avbl Limit: INR 141087.71 " +
            "If not done by you, call 180010888"

        val result = parser.parse("VD-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(14598.0, result!!.amount, 0.01)
        assertEquals("CLEARTRIP PRIVATE LIMI", result.merchant)
        assertEquals("6887", result.accountLast4)
    }

    @Test
    fun `parse credit card spend - Happy Shopping prefix`() {
        val body = "Happy Shopping! INR 795.35 spent on your IDFC FIRST Bank Credit Card " +
            "ending XX6426 at Amazon Pay on 17 FEB 2026 at 04:15 PM Avbl Limit: INR 140042.36 " +
            "If not done by you, call 180010888"

        val result = parser.parse("VM-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(795.35, result!!.amount, 0.01)
        assertEquals("Amazon Pay", result.merchant)
        assertEquals("6426", result.accountLast4)
    }

    @Test
    fun `parse credit card spend - large amount with comma`() {
        val body = "Transaction Successful! INR 3,418.00 spent on your IDFC FIRST Bank Credit Card " +
            "ending XX6426 at Travel Retail servi on 21 FEB 2026 at 09:11 PM Avbl Limit: INR 135763.54"

        val result = parser.parse("AX-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(3418.0, result!!.amount, 0.01)
        assertEquals("Travel Retail servi", result.merchant)
    }

    @Test
    fun `parse savings account debit`() {
        val body = "Your A/C XXXXX632065 is debited by INR 230.00 on 27/01/25 09:15. " +
            "New Bal :INR 25,988.14. Call us on 180010888 for dispute. Team IDFC FIRST Bank"

        val result = parser.parse("JK-IDFCFB", body)
        assertNotNull(result)
        assertEquals(230.0, result!!.amount, 0.01)
        assertEquals("INR", result.currencyCode)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("2065", result.accountLast4)
        assertNull(result.merchant)
    }

    @Test
    fun `parse savings account debit - larger amount`() {
        val body = "Your A/C XXXXX632065 is debited by INR 450.00 on 24/01/25 19:57. " +
            "New Bal :INR 26,218.14. Call us on 180010888 for dispute. Team IDFC FIRST Bank"

        val result = parser.parse("JD-IDFCFB", body)
        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("2065", result.accountLast4)
    }

    @Test
    fun `parse savings account credit`() {
        val body = "Your A/C XXXXX632065 is credited with INR 3,000.00 on 23/01/25 13:24. " +
            "Your new balance is INR 26,668.14. Team IDFC FIRST Bank"

        val result = parser.parse("JX-IDFCFB", body)
        assertNotNull(result)
        assertEquals(3000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("2065", result.accountLast4)
    }

    @Test
    fun `parse card payment confirmation`() {
        val body = "Thank you for payment of INR 32,638.46 towards your " +
            "Mayura Credit Card XX6887 on 05 Mar 2026. IDFC FIRST Bank"

        val result = parser.parse("JD-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(32638.46, result!!.amount, 0.01)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("6887", result.accountLast4)
    }

    @Test
    fun `parse card payment confirmation - different card`() {
        val body = "Thank you for payment of INR 1,761.02 towards your " +
            "FIRST Select Credit Card XX1528 on 05 Feb 2026. IDFC FIRST Bank"

        val result = parser.parse("JD-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(1761.02, result!!.amount, 0.01)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("1528", result.accountLast4)
    }

    @Test
    fun `parse interest credit - INR dot format`() {
        val body = "Monthly interest of INR.62.00 earned on your Savings A/c XX2065 " +
            "has been credited to your A/C on 28/02/26. New bal: INR.26,910.14. IDFC FIRST Bank"

        val result = parser.parse("AD-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(62.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("Interest", result.merchant)
        assertEquals("2065", result.accountLast4)
    }

    @Test
    fun `parse interest credit - Rs dot format`() {
        val body = "Monthly interest of Rs.68.00 earned on your Savings A/c XX2065 " +
            "has been credited to your A/C on 31/01/26. New bal: Rs.26,848.14. IDFC FIRST Bank"

        val result = parser.parse("JD-IDFCFB-S", body)
        assertNotNull(result)
        assertEquals(68.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("Interest", result.merchant)
        assertEquals("2065", result.accountLast4)
    }

    @ParameterizedTest
    @CsvSource(
        "JD-IDFCFB-S, Dear Customer DO NOT share OTP/ CVV/ UPI PIN/ Account details",
        "VD-IDFCFB-S, Your new device has been registered for Mobile Banking",
        "JD-IDFCFB-T, 011623 is the OTP for INR 19719.98 txn at Etraveli I on IDFC FIRST Bank",
        "JD-IDFCFB-S, Your IDFC FIRST Bank Credit Card XXXX1528 is due for renewal",
        "AX-IDFCFB-S, Your Mayura Credit Card XX6887 bill due by 09 March"
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
