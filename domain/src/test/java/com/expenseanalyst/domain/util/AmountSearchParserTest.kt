package com.expenseanalyst.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AmountSearchParserTest {

    @Test
    fun `parses a whole number as an exact amount`() {
        val result = AmountSearchParser.parse("4386")
        assertTrue(result is AmountSearchParser.Result.Exact)
        assertEquals(4386.0, (result as AmountSearchParser.Result.Exact).amount, 0.0)
    }

    @Test
    fun `parses a decimal amount`() {
        val result = AmountSearchParser.parse("45.50")
        assertTrue(result is AmountSearchParser.Result.Exact)
        assertEquals(45.50, (result as AmountSearchParser.Result.Exact).amount, 0.0)
    }

    @Test
    fun `parses a range with the exact wording from the issue`() {
        val result = AmountSearchParser.parse("4000 to 4500")
        assertTrue(result is AmountSearchParser.Result.Range)
        val range = result as AmountSearchParser.Result.Range
        assertEquals(4000.0, range.min, 0.0)
        assertEquals(4500.0, range.max, 0.0)
    }

    @Test
    fun `range is case-insensitive on the word to`() {
        val result = AmountSearchParser.parse("100 TO 200")
        assertTrue(result is AmountSearchParser.Result.Range)
    }

    @Test
    fun `range normalises reversed bounds so min is always the smaller value`() {
        val result = AmountSearchParser.parse("4500 to 4000")
        assertTrue(result is AmountSearchParser.Result.Range)
        val range = result as AmountSearchParser.Result.Range
        assertEquals(4000.0, range.min, 0.0)
        assertEquals(4500.0, range.max, 0.0)
    }

    @Test
    fun `tolerates extra whitespace around a range`() {
        val result = AmountSearchParser.parse("  4000   to   4500  ")
        assertTrue(result is AmountSearchParser.Result.Range)
    }

    @Test
    fun `merchant text is not treated as an amount`() {
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse("Noon"))
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse("lunch"))
    }

    @Test
    fun `a malformed range falls back to not an amount`() {
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse("4000 to"))
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse("to 4500"))
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse("4000 to abc"))
    }

    @Test
    fun `blank query is not an amount`() {
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse(""))
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse("   "))
    }

    @Test
    fun `a number embedded in text is not an exact match`() {
        // "Order 4386 confirmed" should search as text, not be parsed as amount 4386 —
        // the pattern requires the whole trimmed string to be numeric.
        assertEquals(AmountSearchParser.Result.NotAnAmount, AmountSearchParser.parse("Order 4386 confirmed"))
    }
}
