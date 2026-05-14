package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxisBankStatementParserTest {

    private val parser = AxisBankStatementParser()

    @Test
    fun `canParse classic Payment of INR format`() {
        val body = "Payment of INR 8523.16 for Axis Bank Credit Card no. XX3745 is due on 04-04-26 " +
            "with minimum amount due of INR 8523.16. Ignore if paid."
        assertTrue(parser.canParse("AD-AXISBK", body))
    }

    // ── Issue #4 / #7: "INR X is due for payment on" must be classified as a bill ──
    @Test
    fun `canParse newer 'is due for payment on' format`() {
        val body = "INR 10747.2 is due for payment on 10-05-26 towards Axis Bank CC no. XX4502. " +
            "INR 215 will be debited from Axis Bank A/c no. XX0426 via auto debit."
        assertTrue(parser.canParse("AD-AXISBK", body))
    }

    @Test
    fun `parse newer due-for-payment SMS extracts CC totalDue and CC last-four`() {
        val body = "INR 10747.2 is due for payment on 10-05-26 towards Axis Bank CC no. XX4502. " +
            "INR 215 will be debited from Axis Bank A/c no. XX0426 via auto debit."
        val result = parser.parse("AD-AXISBK", body)
        assertNotNull(result)
        assertEquals(10747.2, result!!.totalDue!!, 0.01)
        // CC last-4 takes priority over the auto-debit account last-4
        assertEquals("4502", result.accountLast4)
        assertEquals("INR", result.currencyCode)
        assertEquals("Axis Bank", result.billerName)
        // Due date is 10-05-26 (UTC midnight). We assert non-null; exact value depends on TZ math.
        assertNotNull(result.dueDateMillis)
    }

    @Test
    fun `parse classic Payment of INR SMS still works`() {
        val body = "Payment of INR 8523.16 for Axis Bank Credit Card no. XX3745 is due on 04-04-26 " +
            "with minimum amount due of INR 8523.16. Ignore if paid."
        val result = parser.parse("AD-AXISBK", body)
        assertNotNull(result)
        assertEquals(8523.16, result!!.totalDue!!, 0.01)
        assertEquals(8523.16, result.minimumDue!!, 0.01)
        assertEquals("3745", result.accountLast4)
        assertEquals("INR", result.currencyCode)
    }

    @Test
    fun `canParse false for non-Axis sender`() {
        val body = "INR 1000 is due for payment on 10-05-26 towards HDFC Bank CC no. XX1234."
        assertEquals(false, parser.canParse("HDFCBK", body))
    }

    @Test
    fun `parse returns null for unrelated SMS`() {
        assertNull(parser.parse("AD-AXISBK", "Your OTP is 1234. Do not share."))
    }
}
