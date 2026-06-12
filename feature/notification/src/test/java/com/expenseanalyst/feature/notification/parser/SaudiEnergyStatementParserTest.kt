package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SaudiEnergyStatementParserTest {

    private val parser = SaudiEnergyStatementParser()

    // ── canParse ──────────────────────────────────────────────────────────────

    @Test
    fun `canParse matches standard bill-issued SMS`() {
        val body = "Dear ANOOP, Your bill for account 30143319970 has been issued with amount of 149.43 SAR. You can view and pay your bills via SE app using the following link: https://www.se.com.sa/app"
        assert(parser.canParse("SE", body))
    }

    @Test
    fun `canParse matches unpaid-reminder SMS with issued keyword`() {
        val body = "We would like to remind you that your issued bill for account No. 30166041401 in the amount of 82.92 SAR has not been paid."
        assert(parser.canParse("SE", body))
    }

    @Test
    fun `canParse matches via se-com-sa domain`() {
        assert(parser.canParse("UNKNOWN", "Pay via https://www.se.com.sa/app"))
    }

    @Test
    fun `canParse rejects unrelated SMS`() {
        assert(!parser.canParse("HDFC", "Rs.500 debited from your account."))
    }

    // ── parse: standard bill-issued SMS ──────────────────────────────────────

    @Test
    fun `parse extracts amount and account from standard bill SMS`() {
        val body = "Dear ANOOP, Your bill for account 30143319970 has been issued with amount of 149.43 SAR. https://www.se.com.sa/app"
        val result = parser.parse("SE", body)
        assertNotNull(result)
        assertEquals("Saudi Energy", result!!.billerName)
        assertEquals(149.43, result.totalDue!!, 0.001)
        assertEquals("SAR", result.currencyCode)
        assertEquals("9970", result.accountLast4)
        assertEquals("30143319970", result.reference)
    }

    // ── parse: unpaid-reminder SMS (Issue #15) ────────────────────────────────

    @Test
    fun `parse extracts amount and account from unpaid-reminder SMS`() {
        val body = "We would like to remind you that your issued bill for account No. 30166041401 in the amount of 82.92 SAR has not been paid."
        val result = parser.parse("SE", body)
        assertNotNull(result)
        assertEquals("Saudi Energy", result!!.billerName)
        assertEquals(82.92, result.totalDue!!, 0.001)
        assertEquals("SAR", result.currencyCode)
        assertEquals("1401", result.accountLast4)
        assertEquals("30166041401", result.reference)
    }

    @Test
    fun `parse returns null when amount is missing`() {
        val body = "Your bill for account 30143319970 has been issued. Please pay via se.com.sa"
        assertNull(parser.parse("SE", body))
    }
}
