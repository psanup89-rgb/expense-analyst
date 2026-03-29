package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FasTagParserTest {

    private val parser = FasTagParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "AD-QWFSTG, true",
        "VMQWFSTG, true",
        "QWFSTG-S, true",
        "HDFCBK, false",
        "SBIBNK, false"
    )
    fun `canParse recognises FASTag sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "Livquik Fastag debited Rs25.0"))
    }

    @Test
    fun `parse extracts toll debit with location`() {
        val result = parser.parse("AD-QWFSTG",
            "Livquik Fastag debited Rs25.0 for TN19AM4212 in VANAGARAM at 28/09/24 20:39,Bal Rs338.0.")
        assertNotNull(result)
        assertEquals(25.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("INR", result.currencyCode)
        assertEquals("WALLET", result.paymentMethodName)
        assertNull(result.accountLast4)
        assertEquals("VANAGARAM", result.merchant)
    }

    @Test
    fun `parse extracts parking fee with location`() {
        val result = parser.parse("VMQWFSTG",
            "Customer,Parking fee of Rs.120.0 in 9042173418 is debited from LivQuik Fastag at NEXUS VIJAYA MA, 20/04/25")
        assertNotNull(result)
        assertEquals(120.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("NEXUS VIJAYA MA", result.merchant)
    }

    @Test
    fun `parse returns null when no debit keyword`() {
        val result = parser.parse("AD-QWFSTG",
            "Livquik Fastag balance Rs500.0 for TN19AM4212.")
        assertNull(result)
    }

    @Test
    fun `parse returns null when no amount found`() {
        val result = parser.parse("AD-QWFSTG",
            "Livquik Fastag debited for TN19AM4212 in VANAGARAM at 28/09/24.")
        assertNull(result)
    }
}
