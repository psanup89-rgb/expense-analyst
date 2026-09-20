package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OneCardStatementParserTest {

    private val body = "Your Federal Bank  One Credit Card bill of Rs. 20,809.99 is ready. " +
        "Pay by 07 Oct, 2026 via the OneCard app: https://1crd.in/OneCrd/BillDetails"

    @Test
    fun `parses a OneCard bill-ready message`() {
        val result = OneCardStatementParser().parse("CP-OneCrd-T", body)
        assertNotNull(result)
        assertEquals(20809.99, result!!.totalDue!!, 0.001)
        assertEquals("INR", result.currencyCode)
        assertEquals("OneCard", result.bankName)
        assertNotNull(result.dueDateMillis)
    }

    @Test
    fun `registry routes the OneCard bill message to the OneCard parser`() {
        val result = BillStatementParserRegistry.parse("CP-OneCrd-T", body)
        assertEquals("OneCard", result?.bankName)
        assertEquals(20809.99, result?.totalDue ?: 0.0, 0.001)
    }

    @Test
    fun `does not claim a normal OneCard purchase message`() {
        val purchase = "Hi, You spent Rs. 250.00 at ZOMATO using your Federal One Credit Card."
        assertNull(BillStatementParserRegistry.parse("CP-OneCrd-T", purchase))
    }
}
