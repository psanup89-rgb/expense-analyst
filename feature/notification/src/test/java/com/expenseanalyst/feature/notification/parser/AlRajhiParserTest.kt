package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AlRajhiParserTest {

    private val parser = AlRajhiParser()

    @ParameterizedTest(name = "canParse sender={0}")
    @CsvSource(
        "AlRajhi, true",
        "ALRAJHI, true",
        "74100, true",
        "HDFCBK, false",
        "SBIBNK, false"
    )
    fun `canParse recognises Al Rajhi sender IDs`(sender: String, expected: Boolean) {
        val result = parser.canParse(sender, "SAR 100.00 debited")
        assertEquals(expected, result)
    }

    @ParameterizedTest(name = "parse debit: {1}")
    @CsvSource(
        "AlRajhi, Purchase of SAR 250.00 was made using your card ending 1234 at Jarir Bookstore on 01/01/2025, 250.0, Jarir Bookstore, 1234",
        "AlRajhi, SAR 180.00 debited from card ending 4321 at Extra Stores, 180.0, Extra Stores, 4321",
        "AlRajhi, Online Purchase By:7573 ;Visa Amount:228.24 SAR At:Amazon SA Balance:47242.85 SAR 24/3/26 13:20, 228.24, Amazon SA, 7573",
        "AlRajhi, Online Purchase Card:7573 ;Visa Amount:23USD(86.36 SAR) At: CLAUDE.AI Fee 8VAT: 1.73 SAR Exchange rate~: 3.754783 Total due amount:88.09 SAR Country:USA Balance:47892.18 SAR 25/3/26 21:02, 86.36, CLAUDE.AI, 7573"
    )
    fun `parse extracts SAR debit transactions`(
        sender: String,
        body: String,
        expectedAmount: Double,
        expectedMerchant: String,
        expectedCard: String
    ) {
        val result = parser.parse(sender, body)
        assertNotNull(result)
        assertEquals(expectedAmount, result!!.amount, 0.01)
        assertEquals(TransactionDirection.DEBIT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals(expectedMerchant, result.merchant)
        assertEquals(expectedCard, result.accountLast4)
    }

    @ParameterizedTest
    @CsvSource(
        "AlRajhi, SAR 1500.00 has been credited to your account ending 5678. Ref: 987654321, 1500.0, CREDIT"
    )
    fun `parse extracts SAR credit transactions`(
        sender: String,
        body: String,
        expectedAmount: Double,
        expectedType: String
    ) {
        val result = parser.parse(sender, body)
        assertNotNull(result)
        assertEquals(expectedAmount, result!!.amount, 0.01)
        assertEquals(TransactionDirection.CREDIT, result.type)
        assertEquals("SAR", result.currencyCode)
    }

    @ParameterizedTest
    @CsvSource(
        "AlRajhi, Your OTP for Al Rajhi account is 1234.",
        "AlRajhi, Dear customer your account is active."
    )
    fun `parse returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(parser.parse(sender, body))
    }

    @org.junit.jupiter.api.Test
    fun `parse extracts internal transfer correctly`() {
        val body = """
            Credit Transfer Internal
            Amount:SAR 5000
            To:6805
            From:MOHAMATHU PILLAI
            From:5119
            26/3/29 14:05
        """.trimIndent()
        val result = parser.parse("AlRajhi", body)
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.TRANSFER, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("6805", result.accountLast4)
        assertEquals("MOHAMATHU PILLAI", result.merchant)
        assertEquals("NET_BANKING", result.paymentMethodName)
        assertEquals("Al Rajhi Bank", result.bankName)
    }

    @org.junit.jupiter.api.Test
    fun `canParse returns true for transfer body fingerprint without sender match`() {
        val body = "Credit Transfer Internal\nAmount:SAR 5000\nTo:6805\nFrom:SOMEONE\nFrom:5119"
        assertEquals(true, parser.canParse("74100", body))
    }

    @org.junit.jupiter.api.Test
    fun `parse extracts a standing order transfer, attributed to Al Rajhi not D360`() {
        val body = """
            Standing Order-Debit Transfer Local
            From:6805
            Amount:SAR 5000
            To:ANOOP SASEEDHARAN
            To:1616
        """.trimIndent()
        val result = parser.parse("AlRajhiBank", body)
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.TRANSFER, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("6805", result.accountLast4)
        assertEquals("ANOOP SASEEDHARAN", result.merchant)
        assertEquals("Al Rajhi Bank", result.bankName)
    }

    @org.junit.jupiter.api.Test
    fun `parse extracts a bill payment, attributed to Al Rajhi not Mubasher`() {
        val body = """
            Bill Payment
            From:6805
            Amount:SAR 240
            Biller:125
            Service:ENBD PAYMENTS
            Bill:01600000025919
        """.trimIndent()
        val result = parser.parse("AlRajhiBank", body)
        assertNotNull(result)
        assertEquals(240.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.PAYMENT, result.type)
        assertEquals("SAR", result.currencyCode)
        assertEquals("6805", result.accountLast4)
        assertEquals("ENBD PAYMENTS", result.merchant)
        assertEquals("Al Rajhi Bank", result.bankName)
    }

    @org.junit.jupiter.api.Test
    fun `ParserRegistry attributes a real Al Rajhi standing order to Al Rajhi, not D360`() {
        val body = "Standing Order-Debit Transfer Local\nFrom:6805\nAmount:SAR 5000\nTo:ANOOP SASEEDHARAN\nTo:1616"
        val result = ParserRegistry.parse("AlRajhiBank", body)
        assertEquals("Al Rajhi Bank", result?.bankName)
    }

    @org.junit.jupiter.api.Test
    fun `ParserRegistry attributes a real Al Rajhi bill payment to Al Rajhi, not Mubasher`() {
        val body = "Bill Payment\nFrom:6805\nAmount:SAR 240\nBiller:125\nService:ENBD PAYMENTS\nBill:01600000025919"
        val result = ParserRegistry.parse("AlRajhiBank", body)
        assertEquals("Al Rajhi Bank", result?.bankName)
    }

    @org.junit.jupiter.api.Test
    fun `parse extracts a Debit Internal Transfer as a transfer from the users own account`() {
        val body = "Debit Internal Transfer\nFrom:6805\nAmount:SR 4000\nTo:SAMUEL RAJASEKAR\nTo:0221\n26/9/20 18:05"
        val result = ParserRegistry.parse("AlRajhiBank", body)
        assertNotNull(result)
        assertEquals(4000.0, result!!.amount, 0.01)
        assertEquals(TransactionDirection.TRANSFER, result.type)
        assertEquals("6805", result.accountLast4)
        assertEquals("SAMUEL RAJASEKAR", result.merchant)
        assertEquals("Al Rajhi Bank", result.bankName)
    }
}
