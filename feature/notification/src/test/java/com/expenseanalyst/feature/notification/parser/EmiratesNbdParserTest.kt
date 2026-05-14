package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EmiratesNbdParserTest {

    private val parser = EmiratesNbdParser()

    @ParameterizedTest(name = "canParse sender={0}")
    @CsvSource(
        "EmiratesNBD, true",
        "EMIRATESNBD, true",
        "ENBD, true",
        "emirates nbd, true",
        "AlRajhi, false",
        "HDFCBK, false"
    )
    fun `canParse recognises Emirates NBD sender IDs`(sender: String, expected: Boolean) {
        val result = parser.canParse(sender, "some body text")
        assertEquals(expected, result)
    }

    @Test
    fun `canParse detects Emirates NBD by body fingerprint`() {
        assertTrue(parser.canParse("12345", "POS Purchase (Apple Pay)\nCard: Visa card XX4388\nAmount: SAR 36.00\nMerchant: STARBUCKS"))
    }

    @Test
    fun `parse POS purchase with Apple Pay`() {
        val body = """
            POS Purchase (Apple Pay)
            Card: Visa card XX4388
            Amount: SAR 36.00
            Merchant: STARBUCKS-S876
            In: SAUDI ARABIA
            Remaining limit SAR 18,117.95
            On: 2026-03-28 15:54:43
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(36.0, result!!.amount, 0.01)
        assertEquals("SAR", result.currencyCode)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("STARBUCKS-S876", result.merchant)
        assertEquals("4388", result.accountLast4)
        assertEquals("APPLE_PAY", result.paymentMethodName)
        assertEquals("Emirates NBD", result.bankName)
    }

    @Test
    fun `parse POS purchase Atypical merchant`() {
        val body = """
            POS Purchase (Apple Pay)
            Card: Visa card XX4388
            Amount: SAR 19.00
            Merchant: Atypical
            In: SAUDI ARABIA
            Remaining limit SAR 18,153.95
            On: 2026-03-26 13:30:31
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(19.0, result!!.amount, 0.01)
        assertEquals("Atypical", result.merchant)
        assertEquals("4388", result.accountLast4)
        assertEquals("APPLE_PAY", result.paymentMethodName)
    }

    @Test
    fun `parse Online purchase with Credit card label`() {
        val body = """
            Online Purchase (Apple Pay)
            Card: Credit card XX4388
            Merchant: Temu.com
            Amount: SAR 14.36
            On: 2026-03-24 02:14:13
            Remaining limit SAR 18,172.95
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(14.36, result!!.amount, 0.01)
        assertEquals("SAR", result.currencyCode)
        assertEquals("Temu.com", result.merchant)
        assertEquals("4388", result.accountLast4)
        assertEquals("APPLE_PAY", result.paymentMethodName)
    }

    @Test
    fun `parse POS purchase FADWA COMPANY`() {
        val body = """
            POS Purchase (Apple Pay)
            Card: Visa card XX4388
            Amount: SAR 40.00
            Merchant: FADWA COMPANY
            In: SAUDI ARABIA
            Remaining limit SAR 18,187.31
            On: 2026-03-23 19:04:05
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(40.0, result!!.amount, 0.01)
        assertEquals("FADWA COMPANY", result.merchant)
    }

    @Test
    fun `parse POS purchase pharmacy`() {
        val body = """
            POS Purchase (Apple Pay)
            Card: Visa card XX4388
            Amount: SAR 57.30
            Merchant: Asharq Alawsat Pharmacies
            In: SAUDI ARABIA
            Remaining limit SAR 18,227.31
            On: 2026-03-14 15:35:24
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(57.3, result!!.amount, 0.01)
        assertEquals("Asharq Alawsat Pharmacies", result.merchant)
    }

    @Test
    fun `parse POS purchase medical`() {
        val body = """
            POS Purchase (Apple Pay)
            Card: Visa card XX4388
            Amount: SAR 304.75
            Merchant: Sehat Al Olaya Medical Co
            In: SAUDI ARABIA
            Remaining limit SAR 18,284.61
            On: 2026-03-14 15:10:28
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(304.75, result!!.amount, 0.01)
        assertEquals("Sehat Al Olaya Medical Co", result.merchant)
    }

    @Test
    fun `parse Online purchase HUNGERSTATION`() {
        val body = """
            Online Purchase (Apple Pay)
            Card: Credit card XX4388
            Merchant: HUNGERSTATION LLC
            Amount: SAR 35.60
            On: 2026-03-13 13:39:26
            Remaining limit SAR 18,589.36
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(35.6, result!!.amount, 0.01)
        assertEquals("HUNGERSTATION LLC", result.merchant)
        assertEquals("4388", result.accountLast4)
        assertEquals("APPLE_PAY", result.paymentMethodName)
    }

    @Test
    fun `parse without payment method in parentheses`() {
        val body = """
            POS Purchase
            Card: Visa card XX4388
            Amount: SAR 50.00
            Merchant: SOME STORE
            In: SAUDI ARABIA
            Remaining limit SAR 10,000.00
            On: 2026-03-10 10:00:00
        """.trimIndent()

        val result = parser.parse("EmiratesNBD", body)
        assertNotNull(result)
        assertEquals(50.0, result!!.amount, 0.01)
        assertEquals("SOME STORE", result.merchant)
        // No wallet pay in parentheses, but "Visa card" → inferred as CREDIT_CARD
        assertEquals("CREDIT_CARD", result.paymentMethodName)
    }

    @ParameterizedTest
    @CsvSource(
        "EmiratesNBD, Your OTP is 1234. Do not share with anyone.",
        "EmiratesNBD, Dear customer your card has been activated.",
        "EmiratesNBD, Enjoy 20% cashback on your next purchase!"
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }

    // ── Issue #6: "Credit Card: Credited" payment confirmation ─────────────────
    @Test
    fun `parse Credit Card Credited routes to PAYMENT and is detected without ENBD sender`() {
        val body = """
            Credit Card: Credited
            Card: XX4388;Credit Card Visa
            Amount: SAR 1,320.00
            Balance: SAR 17,172.88
            Date: 03-05-2026
        """.trimIndent()

        // Should be detected via body fingerprint even when sender doesn't say ENBD.
        assertTrue(parser.canParse("12345", body))

        val result = parser.parse("12345", body)
        assertNotNull(result)
        assertEquals(1320.0, result!!.amount, 0.01)
        assertEquals("SAR", result.currencyCode)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("4388", result.accountLast4)
        assertEquals("CREDIT_CARD", result.paymentMethodName)
        assertEquals("Emirates NBD", result.bankName)
    }

    // ── Issue #9: POS Reversal refund — detected via body fingerprint ──────────
    @Test
    fun `parse POS Reversal classifies as CREDIT with merchant from From line`() {
        val body = """
            POS Reversal
            To: XX4388; Visa Credit
            Amount: SAR 64.96
            From: UBR* PENDING.UBER.COM
            In NETHERLANDS
            Remaining limit SAR: 14,959.53
            On: 2026-05-08 00:47:44
        """.trimIndent()

        // Should be detected even when the sender ID lacks "ENBD".
        assertTrue(parser.canParse("12345", body))

        val result = parser.parse("12345", body)
        assertNotNull(result)
        assertEquals(64.96, result!!.amount, 0.01)
        assertEquals("SAR", result.currencyCode)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("4388", result.accountLast4)
        assertEquals("UBR* PENDING.UBER.COM", result.merchant)
    }
}
