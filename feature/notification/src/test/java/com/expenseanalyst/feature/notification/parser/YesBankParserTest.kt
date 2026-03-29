package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class YesBankParserTest {

    private val parser = YesBankParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "YESBNK, true",
        "YESBK, true",
        "AD-YESBNK, true",
        "HDFCBK, false",
        "SBIBNK, false"
    )
    fun `canParse requires YES BANK in sender and body`(sender: String, expected: Boolean) {
        val body = if (expected) "INR 300.00 debited from YES BANK A/c XX3456" else "Rs 300.00 debited"
        assertEquals(expected, parser.canParse(sender, body))
    }

    @Test
    fun `canParse returns false when body lacks YES BANK`() {
        assertFalse(parser.canParse("YESBNK", "Rs 300.00 debited from A/c XX3456"))
    }

    @Test
    fun `parse extracts debit with merchant from for-pattern`() {
        val result = parser.parse("YESBNK",
            "INR 300.00 has been debited from your YES BANK A/c ending XX3456 for Swiggy. Avl Bal INR 6000.00")
        assertNotNull(result)
        assertEquals(300.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("3456", result.accountLast4)
        assertEquals("Swiggy", result.merchant)
    }

    @Test
    fun `parse extracts credit`() {
        val result = parser.parse("YESBNK",
            "INR 500.00 has been credited to your YES BANK A/c ending XX3456. Ref 778899001")
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("3456", result.accountLast4)
    }

    @Test
    fun `parse extracts NEFT credit with compact account format`() {
        val result = parser.parse("YESBNK",
            "INR 7500.00 credited to YES BANK Ac X2919 on 01MAR25 05:01. NEFT:HDFCN52025030187729571/From:P S ANOOP-MY YES BAN. Bal INR 84612.65.")
        assertNotNull(result)
        assertEquals(7500.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("2919", result.accountLast4)
    }

    @Test
    fun `parse extracts UPI debit with compact account format`() {
        val result = parser.parse("YESBNK",
            "YES BANK Ac X2919 debited for INR 1000.00 on 14FEB25 11:40. UPI:504572454716/To:ps.anup.89-1@okhdfcbank. Bal INR 69712.65.")
        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("2919", result.accountLast4)
    }

    @ParameterizedTest
    @CsvSource(
        "YESBNK, Your YES BANK OTP is 445566.",
        "YESBK, Please update your YES BANK KYC."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
