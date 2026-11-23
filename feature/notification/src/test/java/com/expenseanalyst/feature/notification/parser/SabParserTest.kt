package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SabParserTest {

    private val sentence = "PoS Purchase\nSAB mada(Apple Pay) Card (1234) from (***567) was used at Soul Moment Est for SAR 18.00 on 2026-11-23 16:33"

    @Test
    fun `the sentence layout is a SAB card purchase with the card digits`() {
        for (sender in listOf("SAB", "")) {
            val r = ParserRegistry.parse(sender, sentence)
            assertNotNull(r, "sender='$sender'")
            assertEquals("SAB", r!!.bankName)
            assertEquals(TransactionDirection.DEBIT, r.type)
            assertEquals(18.0, r.amount, 0.001)
            assertEquals("Soul Moment Est", r.merchant)
            assertEquals("1234", r.accountLast4)
            assertEquals("APPLE_PAY", r.paymentMethodName)
        }
    }

    @Test
    fun `a merchant name with spaces and a larger amount`() {
        val r = ParserRegistry.parse(
            "",
            "PoS Purchase\nSAB mada(Apple Pay) Card (1234) from (***567) was used at SASCO Station Compa for SAR 1,131.36 on 2026-11-23 20:47"
        )!!
        assertEquals("SASCO Station Compa", r.merchant)
        assertEquals(1131.36, r.amount, 0.001)
    }

    @Test
    fun `the field layout still reads`() {
        val r = ParserRegistry.parse(
            "",
            "Online Purchase\nBy: ***1234;mada\nFrom: ***5678\nAmount: SAR 64.17\nAt: HUNGERSTATION LLC\nDate: 2026-08-09 20:00:00"
        )!!
        assertEquals("SAB", r.bankName)
        assertEquals("1234", r.accountLast4)
        assertEquals(64.17, r.amount, 0.001)
    }
}
