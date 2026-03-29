package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UpiParserTest {

    private val parser = UpiParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "gpay, true",
        "googlepay, true",
        "phonepe, true",
        "paytm, true",
        "bharatpe, true",
        "HDFCBK, false"
    )
    fun `canParse recognises UPI app senders`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "Rs. 200 paid to Swiggy"))
    }

    @Test
    fun `canParse true from UPI body fingerprint`() {
        assertTrue(parser.canParse("UNKNOWN", "Rs 500 debited via UPI. UPI Ref: 123456"))
    }

    @Test
    fun `parse extracts Google Pay payment to merchant`() {
        // "paid to" pattern captures merchant directly; "you paid" captures everything before "via"
        val result = parser.parse("gpay",
            "Rs 200 paid to Swiggy via Google Pay. UPI Ref: 123456789012")
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("Swiggy", result.merchant)
        assertEquals("123456789012", result.referenceNumber)
    }

    @Test
    fun `parse extracts PhonePe payment`() {
        val result = parser.parse("phonepe",
            "Rs 150.00 paid to Zomato via PhonePe. Transaction ID: T2501011234")
        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("Zomato", result.merchant)
    }

    @Test
    fun `parse extracts Paytm payment with rupee symbol`() {
        val result = parser.parse("paytm",
            "Rs. 80 paid to Auto Rickshaw via Paytm UPI. Txn ID: PAY2501011234")
        assertNotNull(result)
        assertEquals(80.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
    }

    @Test
    fun `parse extracts UPI credit received`() {
        val result = parser.parse("gpay",
            "Rs 500.00 received from Rahul via Google Pay. UPI Ref: 987654321098")
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("INR", result.currencyCode)
    }

    @Test
    fun `parse extracts amount with rupee symbol`() {
        val result = parser.parse("gpay", "You paid \u20b9200 to Swiggy via Google Pay. UPI Ref: 111222333")
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.01)
    }

    @ParameterizedTest
    @CsvSource(
        "gpay, Your Google Pay is set up and ready.",
        "phonepe, PhonePe account verified successfully."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
