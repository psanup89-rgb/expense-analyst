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
}
