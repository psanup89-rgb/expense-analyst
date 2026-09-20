package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AlRajhiStatementParserTest {

    private val body = """
        Credit Card: June Statement
        Card: 7573
        Total amount due: SAR 9353.03
        Minimum amount due: SAR 467.66
        You can also pay your card dues using the SADAD number: 991.
        Due date: 25-07-2026
    """.trimIndent()

    /**
     * Regression: the due-date regex used to treat "date|by" as optional, so it matched the
     * earlier "Total amount due:" and tried to parse the amount as a date.
     */
    @Test
    fun `reads the real due date, not the amount-due line`() {
        val result = AlRajhiStatementParser().parse("AlRajhiBank", body)
        assertNotNull(result)
        assertEquals(9353.03, result!!.totalDue!!, 0.001)

        val expected = LocalDate.of(2026, 7, 25).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(expected, result.dueDateMillis)
    }

    @Test
    fun `statement still parses when no due date is present`() {
        val noDate = "Credit Card: June Statement\nCard: 7573\nTotal amount due: SAR 9353.03"
        val result = AlRajhiStatementParser().parse("AlRajhiBank", noDate)
        assertNotNull(result)
        assertEquals(null, result!!.dueDateMillis)
    }
}
