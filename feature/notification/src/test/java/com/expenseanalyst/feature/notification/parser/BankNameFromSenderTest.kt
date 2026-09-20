package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BankNameFromSenderTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        // The split that put one Al Rajhi card under two biller names
        "AlRajhiBank, Al Rajhi Bank",
        "AD-ALRJHI-S, Al Rajhi Bank",
        // Spaceless app sender vs DLT code, previously two different billers
        "EmiratesNBD, Emirates NBD",
        "AD-EMIRNBD-S, Emirates NBD",
        // 6-char DLT truncations that contain neither full word
        "CP-FEDONE-S, OneCard",
        "AX-OneCrd-S, OneCard",
        "JM-DBSBNK-S, DBS Bank",
        "VD-HDFCBK-T, HDFC Bank"
    )
    fun `resolves sender ids to one canonical bank name`(sender: String, expected: String) {
        assertEquals(expected, BankNameFromSender.resolve(sender))
    }

    @Test
    fun `returns null for a sender that is not a known bank`() {
        assertNull(BankNameFromSender.resolve("AD-ZOMATO-S"))
        assertNull(BankNameFromSender.resolve(""))
    }

    @Test
    fun `generic statement parser files a bill under the canonical biller name`() {
        val body = "Your credit card statement is ready. Total amount due: SAR 9270.04. Due date: 25-07-2026"
        val result = BillStatementParserRegistry.parse("AlRajhiBank", body)
        assertEquals("Al Rajhi Bank", result?.billerName)
    }
}
