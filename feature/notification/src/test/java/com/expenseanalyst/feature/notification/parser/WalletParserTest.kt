package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WalletParserTest {

    private val parser = WalletParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "Apple Pay, true",
        "Google Pay, true",
        "Samsung Pay, true",
        "Google Wallet, true",
        "HDFCBK, false"
    )
    fun `canParse recognises wallet senders`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "$12.50 at Starbucks"))
    }

    @Test
    fun `canParse true from body fingerprint`() {
        assertTrue(parser.canParse("UNKNOWN", "Apple Pay - $12.50 at Starbucks"))
    }

    @Test
    fun `parse extracts Apple Pay USD spend`() {
        val result = parser.parse("Apple Pay", "Apple Pay: $12.50 paid at Starbucks")
        assertNotNull(result)
        assertEquals(12.5, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("USD", result.currencyCode)
        assertEquals("Starbucks", result.merchant)
    }

    @Test
    fun `parse extracts Google Pay send`() {
        val result = parser.parse("Google Pay", "Google Pay: $8.00 sent to John Doe")
        assertNotNull(result)
        assertEquals(8.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("USD", result.currencyCode)
        assertEquals("John Doe", result.merchant)
    }

    @Test
    fun `parse extracts Samsung Pay purchase`() {
        val result = parser.parse("Samsung Pay", "Samsung Pay: Payment of $45.00 approved at Target")
        assertNotNull(result)
        assertEquals(45.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("USD", result.currencyCode)
        assertEquals("Target", result.merchant)
    }

    @Test
    fun `parse extracts AED spend`() {
        val result = parser.parse("Apple Pay", "Apple Pay: AED 55.00 payment approved at Carrefour")
        assertNotNull(result)
        assertEquals(55.0, result!!.amount, 0.01)
        assertEquals("AED", result.currencyCode)
    }

    @Test
    fun `parse extracts credit refund`() {
        val result = parser.parse("Apple Pay", "Apple Pay: $15.00 refund received from Starbucks")
        assertNotNull(result)
        assertEquals(15.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "Apple Pay, Your Apple Pay is set up.",
        "Google Pay, Google Pay is ready to use."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
