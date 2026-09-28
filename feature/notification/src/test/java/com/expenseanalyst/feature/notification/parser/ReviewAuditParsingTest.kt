package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * Real layouts found in the Review tab audit (29 Sep 2026), names/numbers changed. Several were
 * being read the wrong way round (a refund as a purchase, interest as a debit).
 */
class ReviewAuditParsingTest {

    private fun p(sender: String, body: String) = requireNotNull(ParserRegistry.parse(sender, body)) { body.take(40) }

    @Test
    fun `SAB mada purchase is SAB's, with a clean merchant and debit card`() {
        val body = "Online Purchase\nBy: ***1234;mada\nFrom: ***5678\nAmount: SAR 64.17\nAt: HUNGERSTATION LLC××\nDate: 2026-08-09 20:00:00"
        for (sender in listOf("SAB", "")) {
            val r = p(sender, body)
            assertEquals("SAB", r.bankName, sender)
            assertEquals(TransactionDirection.DEBIT, r.type)
            assertEquals("HUNGERSTATION LLC", r.merchant)
            assertEquals("1234", r.accountLast4)
            assertEquals("DEBIT_CARD", r.paymentMethodName)
        }
        assertFalse(EmiratesNbdParser().canParse("", body))
    }

    @Test
    fun `Emirates NBD POS refund is a credit from the merchant`() {
        val r = p("EmiratesNBD", "POS Refund\nTo: XX1234; Visa Credit\nAmount: SAR 4.60\nFrom: Lulu Supermarket\nIn SAUDI ARABIA\nRemaining limit SAR: 1000.00\nOn: 2026-08-09 10:00:00")
        assertEquals(TransactionDirection.CREDIT, r.type)
        assertEquals("Lulu Supermarket", r.merchant)
        assertEquals("1234", r.accountLast4)
    }

    @Test
    fun `Al Rajhi credit card refund is a credit from the merchant`() {
        val r = p("AlRajhiBank", "Credit Card Refund\nCard: 1234; Visa\nAmount: 39.98 SAR\nFrom: Amazon SA \nDate: 31/8/26 10:00")
        assertEquals(TransactionDirection.CREDIT, r.type)
        assertEquals("Amazon SA", r.merchant)
    }

    @Test
    fun `STC refund is money back, not a purchase`() {
        val r = p("STC Bank", "Refund Online Purchase\nTransaction Amount 8.95 SAR\nFor Hungerstation Online Payment\nOnline Payment \nAccount number ******1234\nDate 18/08/2026 Time 10:00")
        assertEquals(TransactionDirection.CREDIT, r.type)
        assertEquals("Hungerstation Online Payment", r.merchant)
    }

    @Test
    fun `Amazon's own refund notice is named Amazon`() {
        val r = p("AMAZON", "Refund Issued: Amount SAR 39.98 (Dettol Sensitive Liquid Handwash...) will be credited to your Visa within 3-5 business days. Details https://amznsa.com/x")
        assertEquals("Amazon", r.merchant)
    }

    @Test
    fun `HDFC interest deposit is income named Interest`() {
        val r = p("HDFCBK", "Update! INR 7,384.00 deposited in HDFC Bank A/c XX1234 on 30-JUN-26 for Interest paid till 29-JUN-2026.Avl bal INR 1,00,000.00 Cheque deposits in A/C are subject to clearing")
        assertEquals(TransactionDirection.CREDIT, r.type)
        assertEquals("Interest", r.merchant)
    }

    @Test
    fun `an ACH auto-debit is net banking`() {
        val r = p("HDFCBK", "UPDATE: INR 56,700.00 debited from HDFC Bank XX1234 on 05-SEP-26. Info: ACH D- HDFC BANK LTD-123456789. Avl bal:INR 1,00,000.00")
        assertEquals("NET_BANKING", r.paymentMethodName)
        assertEquals("HDFC BANK LTD", r.merchant)
    }

    @Test
    fun `a returned NACH debit is not a transaction`() {
        org.junit.jupiter.api.Assertions.assertNull(ParserRegistry.parse(
            "AX-AXISBK",
            "NACH debit towards MONTHLYSMALLCAS for INR 2,200.00 with UMRN UTIB0000 has been returned today - Axis Bank"
        ))
    }
}
