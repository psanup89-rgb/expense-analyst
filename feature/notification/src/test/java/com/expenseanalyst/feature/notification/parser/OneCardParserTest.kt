package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OneCardParserTest {

    private val parser = OneCardParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "AD-OneCrd, true",
        "CPOneCrd, true",
        "OneCrd-S, true",
        "HDFCBK, false",
        "ICICIB, false"
    )
    fun `canParse recognises OneCard sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "paid AED 24.99 at Noon"))
    }

    @Test
    fun `parse extracts AED spend with card ending`() {
        val result = parser.parse("AD-OneCrd",
            "You've paid AED 24.99 at Noon with your Federal One Credit Card ending in XX3550 and earned reward points!")
        assertNotNull(result)
        assertEquals(24.99, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("AED", result.currencyCode)
        assertEquals("3550", result.accountLast4)
        assertEquals("Noon", result.merchant)
        assertEquals("CREDIT_CARD", result.paymentMethodName)
    }

    @Test
    fun `parse extracts INR payment received`() {
        val result = parser.parse("CPOneCrd",
            "Hola! that was sweet. We have received payment against your OneCard for Rs. 13388.20 on 25 Mar 2026.")
        assertNotNull(result)
        assertEquals(13388.20, result!!.amount, 0.01)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("INR", result.currencyCode)
    }

    @Test
    fun `parse extracts INR refund from merchant`() {
        val result = parser.parse("AD-OneCrd",
            "Hi We have received a refund of Rs. 794.48 from ZOMATO on your Federal One Credit Card.")
        assertNotNull(result)
        assertEquals(794.48, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("ZOMATO", result.merchant)
    }

    @ParameterizedTest
    @CsvSource(
        "AD-OneCrd, Your OneCard OTP is 123456.",
        "CPOneCrd, Your OneCard statement for March 2026 is now available."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
