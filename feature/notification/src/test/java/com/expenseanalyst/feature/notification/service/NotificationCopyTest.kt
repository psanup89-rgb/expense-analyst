package com.expenseanalyst.feature.notification.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class NotificationCopyTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "PAUL CAFE, Paul Cafe",
        "LULU EXPRESS DIPLOMATIC Q, Lulu Express Diplomatic Q",
        "AGODA, Agoda",
        "Keeta Technologies Arabia, Keeta Technologies Arabia",
        "APPLE.COM/BILL, Apple.com/bill",
        "3FIVE8 TECHNOLOGIES, 3five8 Technologies"
    )
    fun `all-caps merchants are title-cased, mixed case is left alone`(raw: String, expected: String) {
        assertEquals(expected, NotificationCopy.displayMerchant(raw))
    }

    @Test
    fun `blank merchant is null`() {
        assertNull(NotificationCopy.displayMerchant("  "))
        assertNull(NotificationCopy.displayMerchant(null))
    }

    @Test
    fun `title leads with the amount and marks money in`() {
        assertEquals("SAR42.00 · Paul Cafe", NotificationCopy.expenseTitle("SAR42.00", "PAUL CAFE", incoming = false))
        assertEquals("+SAR3,720.00 · Salary", NotificationCopy.expenseTitle("SAR3,720.00", "Salary", incoming = true))
        assertEquals("SAR5.00", NotificationCopy.expenseTitle("SAR5.00", null, incoming = false))
    }

    @Test
    fun `body names category and bank, never an unknown bank`() {
        assertEquals("Food & Drinks · Emirates NBD", NotificationCopy.expenseBody("Food & Drinks", "Emirates NBD", false))
        assertEquals("Misc", NotificationCopy.expenseBody("Misc", "Unknown Bank", false))
        assertEquals("Needs review · tap to finish", NotificationCopy.expenseBody("Misc", "Emirates NBD", true))
    }
}
