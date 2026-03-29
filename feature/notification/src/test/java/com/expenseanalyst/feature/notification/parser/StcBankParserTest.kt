package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class StcBankParserTest {

    private val parser = StcBankParser()

    @ParameterizedTest(name = "canParse sender={0} => {1}")
    @CsvSource(
        "STCPay, true",
        "STCBank, true",
        "STCPAY, true",
        "stcpay, true",
        "HDFCBK, false",
        "AlRajhi, false"
    )
    fun `canParse recognises STC sender IDs`(sender: String, expected: Boolean) {
        assertEquals(expected, parser.canParse(sender, "SAR 150.00 paid"))
    }

    @Test
    fun `parse extracts SAR payment with merchant`() {
        val result = parser.parse("STCPay",
            "SAR 150.00 has been paid from your STC Pay account to Noon. Ref: TXN123456")
        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("TXN123456", result.referenceNumber)
    }

    @Test
    fun `parse extracts SAR received with sender name`() {
        val result = parser.parse("STCPay",
            "SAR 200.00 received in your STC Pay account from Ahmed. Ref: TXN789012")
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("TXN789012", result.referenceNumber)
    }

    @Test
    fun `parse extracts amount with decimal`() {
        val result = parser.parse("STCBank",
            "SAR 75.50 has been debited from your STC account. Ref: TXN000111")
        assertNotNull(result)
        assertEquals(75.5, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "STCPay, Your STC Pay OTP is 778899.",
        "STCBank, Your STC account is verified."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }
}
