package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests [TabbyTamaraParser] against its own best-guess message format — this proves the parser
 * behaves as designed, NOT that it matches a real Tabby/Tamara SMS. No real purchase-confirmation
 * sample was available when this was written. See the parser's KDoc.
 */
class TabbyTamaraParserTest {

    private val parser = TabbyTamaraParser()

    @Test
    fun `canParse true for Tamara-style split confirmation`() {
        val body = "Your purchase of 400.00 SAR at Noon has been split into 4 payments of " +
            "100.00 SAR. First payment charged today."
        assertTrue(parser.canParse("Tamara", body))
    }

    @Test
    fun `canParse true for Tabby-style interest-free wording`() {
        val body = "You've split your 400 SAR purchase into 4 interest-free instalments of " +
            "100 SAR with Tabby."
        assertTrue(parser.canParse("Tabby", body))
    }

    @Test
    fun `canParse false without a split-installment fingerprint`() {
        assertFalse(parser.canParse("Tamara", "Welcome to Tamara! Shop now, pay later."))
    }

    // Guards against ever intercepting TamaraStatementParser's due-reminder shape, even though
    // BillStatementParserRegistry already runs before this parser's registry and would claim it
    // first in practice — this is a regression guard, not a live conflict today.
    @Test
    fun `canParse false for a due-date reminder, not a purchase confirmation`() {
        val body = "Reminder! you have a payment of 516.51 SAR for your Samsung order due in " +
            "2 days. Pay now to improve your credit limits: https://tamara.go.link/aQRmC"
        assertFalse(parser.canParse("Tamara", body))
    }

    @Test
    fun `parse extracts total, currency, merchant, direction and provider for Tamara wording`() {
        val body = "Your purchase of 400.00 SAR at Noon has been split into 4 payments of " +
            "100.00 SAR. First payment charged today."
        val result = parser.parse("Tamara", body)
        assertNotNull(result)
        assertEquals(400.00, result!!.amount, 0.01)
        assertEquals("SAR", result.currencyCode)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("Noon", result.merchant)
        assertEquals("Tamara", result.bankName)
        assertTrue(result.isBnplConfirmation)
    }

    @Test
    fun `parse extracts provider Tabby and merchant via order wording`() {
        val body = "You've split your purchase for your Amazon order into 4 interest-free " +
            "instalments of 100 SAR with Tabby."
        val result = parser.parse("Tabby", body)
        assertNotNull(result)
        assertEquals("Tabby", result!!.bankName)
        assertEquals("Amazon", result.merchant)
    }

    @Test
    fun `parse computes total from installment count and amount when no total is stated`() {
        val body = "You've split your purchase into 3 interest-free instalments of 50.00 SAR " +
            "with Tabby."
        val result = parser.parse("Tabby", body)
        assertNotNull(result)
        assertEquals(150.00, result!!.amount, 0.01)
    }

    @Test
    fun `parse returns null when no installment amount is present`() {
        assertNull(parser.parse("Tamara", "Your order has been split into payments."))
    }

    // ── Real wording (27 Sep 2026 sample; link replaced) ──

    private val realConfirmation =
        "Your SAR 599.00 purchase at CENTREPOINT is confirmed. Track your upcoming payments with the Tabby app: https://example.invalid/x"

    @Test
    fun `real Tabby confirmation is recognised as a BNPL purchase`() {
        val parser = TabbyTamaraParser()
        assertTrue(parser.canParse("Tabby", realConfirmation))
        val parsed = parser.parse("Tabby", realConfirmation)!!
        assertEquals(599.0, parsed.amount)
        assertEquals("SAR", parsed.currencyCode)
        assertEquals("CENTREPOINT", parsed.merchant)   // not "CENTREPOINT is confirmed"
        assertEquals("Tabby", parsed.bankName)
        assertEquals(TransactionDirection.PAYMENT, parsed.type)
        assertTrue(parsed.isBnplConfirmation)
    }

    @Test
    fun `multi-word shop names and thousands separators parse`() {
        val body = "Your SAR 1,878.00 purchase at Pan Emirates Riyadh KSA is confirmed. Track your upcoming payments with the Tabby app"
        val parsed = TabbyTamaraParser().parse("Tabby", body)!!
        assertEquals(1878.0, parsed.amount)
        assertEquals("Pan Emirates Riyadh KSA", parsed.merchant)
    }

    @Test
    fun `the card charge to Tabby is not a confirmation`() {
        // The instalment itself — must stay a normal card expense, not be swallowed here.
        val charge = "PoS purchase Card:0000 ;Visa At: Tabby Amount:149.75 SAR Balance: 100.00 SAR 27/9/26 00:30"
        assertFalse(TabbyTamaraParser().canParse("AlRajhiBank", charge))
    }

    @Test
    fun `the registry routes the real confirmation to this parser, ahead of GenericParser`() {
        val parsed = ParserRegistry.parse(sender = "Tabby", body = realConfirmation)!!
        assertTrue(parsed.isBnplConfirmation)
        assertEquals("CENTREPOINT", parsed.merchant)
    }

    // Tamara's real wording (owner's sample, 27 Sep 2026). Never names Tamara in the text.
    private val tamaraConfirmation =
        "Split in 3 payment\nconfirmation:\nStore: Ikea Store\nOrder: 1,585.75 SAR\nDate: 22/09/2026\nVisit the app for more details."

    @org.junit.jupiter.api.Test
    fun `Tamara split confirmation is a Split Payments purchase with its shop and count`() {
        for (sender in listOf("Tamara", "")) {
            val result = ParserRegistry.parse(sender, tamaraConfirmation)
            org.junit.jupiter.api.Assertions.assertNotNull(result, "sender='$sender'")
            org.junit.jupiter.api.Assertions.assertEquals(1585.75, result!!.amount, 0.001)
            org.junit.jupiter.api.Assertions.assertEquals("SAR", result.currencyCode)
            org.junit.jupiter.api.Assertions.assertEquals("Ikea Store", result.merchant)
            org.junit.jupiter.api.Assertions.assertEquals("Tamara", result.bankName)
            org.junit.jupiter.api.Assertions.assertEquals(TransactionDirection.PAYMENT, result.type)
            org.junit.jupiter.api.Assertions.assertTrue(result.isBnplConfirmation)
        }
        org.junit.jupiter.api.Assertions.assertEquals(3, TabbyTamaraParser().instalmentCountOf(tamaraConfirmation))
    }

    @org.junit.jupiter.api.Test
    fun `the Tamara due-reminder parser does not claim a purchase confirmation`() {
        org.junit.jupiter.api.Assertions.assertFalse(TamaraStatementParser().canParse("Tamara", tamaraConfirmation))
    }
}
