package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

class IciciStatementParserTest {

    private val body = "ICICI Bank Credit Card XX1234 Statement is sent to ab******12@gmail.com. " +
        "Total of Rs 1,22,099.00 or minimum of Rs 6,105.00 is due by 17-NOV-26."

    @Test
    fun `the statement is a bill with total, minimum, due date and card`() {
        val s = BillStatementParserRegistry.parse("JD-ICICIT-S", body)
        assertNotNull(s)
        assertEquals("ICICI Bank", s!!.billerName)
        assertEquals(122099.0, s.totalDue!!, 0.001)
        assertEquals(6105.0, s.minimumDue!!, 0.001)
        assertEquals("INR", s.currencyCode)
        assertEquals("1234", s.accountLast4)
        assertEquals(
            LocalDate.of(2026, 11, 17).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            s.dueDateMillis
        )
    }

    @Test
    fun `the statement is never read as a spend`() {
        assertNull(ParserRegistry.parse("JD-ICICIT-S", body))
        assertNull(ParserRegistry.parse("", body))
    }

    @Test
    fun `an ICICI card purchase is still a purchase`() {
        val r = ParserRegistry.parse(
            "JD-ICICIT-S",
            "Rs 1,22,099.00 spent on ICICI Bank Card XX1234 on 10-Oct-26 at WWWCROMACOM. Avl Lmt: Rs 12,345.00."
        )
        assertNotNull(r)
        assertEquals(122099.0, r!!.amount, 0.001)
    }
}
