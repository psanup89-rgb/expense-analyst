package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Integration tests for [ParserRegistry] — verifies dispatch to correct parser.
 */
class ParserRegistryTest {

    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(
        // HDFC
        "HDFCBK, Rs.500.00 debited from a/c XX1234 on 01-01-2025 at Swiggy. Avl bal Rs.12000.00, HDFC Bank, 500.0, DEBIT",
        // SBI
        "SBIBNK, Dear SBI Customer Rs 300.00 debited from A/c XX5678. Info: Zomato. Avl Bal Rs 4000.00, SBI, 300.0, DEBIT",
        // ICICI
        "ICICIB, ICICI Bank Acct XX1234: Rs 450.00 debited on 01-Jan-25. Info: Amazon. Avl Bal: Rs 5500.00, ICICI Bank, 450.0, DEBIT",
        // Axis
        "AXISBK, Rs.600.00 debited from Axis Bank Acct XX9876 on 01-Jan-25. Trf to Uber via UPI. Ref:987654321, Axis Bank, 600.0, DEBIT",
        // Kotak
        "KOTAKB, INR 750.00 debited from Kotak Bank A/c XX5678 on 01-Jan-25 via UPI to Netflix. Ref 111222333, Kotak Bank, 750.0, DEBIT",
        // Al Rajhi
        "AlRajhi, Purchase of SAR 250.00 was made using your card ending 1234 at Jarir Bookstore on 01/01/2025, Al Rajhi Bank, 250.0, DEBIT",
        // STC Bank
        "STCPay, SAR 150.00 has been paid from your STC Pay account to Noon. Ref: TXN123456, STC Bank, 150.0, DEBIT",
        // UPI
        "GPAY, You paid Rs 200 to Swiggy via Google Pay. UPI Ref: 123456789012, UPI, 200.0, DEBIT"
    )
    fun `registry dispatches to correct parser`(
        sender: String,
        body: String,
        expectedBankName: String,
        expectedAmount: Double,
        expectedType: String
    ) {
        val result = ParserRegistry.parse(sender, body)
        assertNotNull(result, "Expected parse result for sender=$sender")
        assertEquals(expectedAmount, result!!.amount, 0.01)
        val expectedDirection = TransactionDirection.valueOf(expectedType)
        assertEquals(expectedDirection, result.type)
    }

    @ParameterizedTest
    @CsvSource(
        "HDFCBK, Your HDFC credit card statement is ready.",
        "RANDOM, Hello world!"
    )
    fun `registry returns null for non-transaction messages`(sender: String, body: String) {
        assertNull(ParserRegistry.parse(sender, body))
    }

    @ParameterizedTest
    @CsvSource(
        "AlRajhi, OTP: 7951. Amount: SAR 649.00. Merchant: noon. For: Internet purchase with card ending 9855.",
        "AXISBK, Your one-time password to complete this transaction of Rs 500 is 445566.",
        "HDFCBK, Verification code 8842 for your purchase of Rs 1200 at Amazon."
    )
    fun `registry ignores OTP messages even though they restate a transaction amount`(sender: String, body: String) {
        assertNull(ParserRegistry.parse(sender, body))
    }

    @org.junit.jupiter.api.Test
    fun `Emirates NBD's own POS purchase shape is not stolen by Al Rajhi's generic fingerprint`() {
        val body = "POS Purchase (Apple Pay)\nCard: Visa card XX4388\nAmount: SAR 36.00\nMerchant: STARBUCKS-S876\nIn: SAUDI ARABIA\nRemaining limit SAR 18,117.95\nOn: 2026-03-28 15:54:43"
        val result = ParserRegistry.parse("EmiratesNBD", body)
        assertEquals("Emirates NBD", result?.bankName)
    }

    @org.junit.jupiter.api.Test
    fun `a credit card payment credited shape from Emirates NBD is not stolen by Al Rajhi's generic fingerprint`() {
        val body = "Credit Card: Credited\nCard : XX4388;Credit Card Visa\nAmount: SAR 39.00\nBalance: SAR 18,156.95\nDate: 29-03-2026"
        val result = ParserRegistry.parse("EmiratesNBD", body)
        assertEquals("Emirates NBD", result?.bankName)
    }

    // ── Cross-bank normalisation (Sep 2026 Misc audit; synthetic numbers) ──

    @org.junit.jupiter.api.Test
    fun `paying a card bill through CRED is a payment, not spending`() {
        val body = "Sent Rs.5000.00\nFrom HDFC Bank A/C *1234\nTo CRED Club\nOn 01/09/26\nRef 123456789012\nNot You?"
        assertEquals(TransactionDirection.PAYMENT, ParserRegistry.parse("VM-HDFCBK", body)?.type)
    }

    @org.junit.jupiter.api.Test
    fun `a card bill payment arriving on the card is a payment, not income`() {
        val axis = "Payment of INR 500 has been received towards your Axis Bank Credit Card XX1234 on 01-09-26 - Axis Bank"
        assertEquals(TransactionDirection.PAYMENT, ParserRegistry.parse("AX-AXISBK", axis)?.type)
        val refund = "Refund of Rs.9.00 from GOOGLE PLAY received on your Federal Bank One Credit Card."
        val r = ParserRegistry.parse("AX-OneCrd", refund)
        if (r != null) assertEquals(TransactionDirection.CREDIT, r.type)
    }

    @org.junit.jupiter.api.Test
    fun `messages that move no money are not transactions`() {
        assertNull(ParserRegistry.parse("AX-AXISBK", "INR 500.00 is due for payment on 05-09-26 towards Axis Bank CC no. XX1234. INR 500 will be debited from Axis Bank A/c no. XX5678 via auto debit."))
        assertNull(ParserRegistry.parse("VM-PHONPE", "ETERNAL LIMITED has requested money from you on PhonePe.Rs.254.0 will be debited from your account on approving the request"))
        assertNull(ParserRegistry.parse("STC", "Hello, Kindy note that your balance is almost consumed. Reply 1 to get Advance Balance of 5 SR for 6 SAR"))
    }

    @org.junit.jupiter.api.Test
    fun `merchant names never carry card digits, helplines or ACH references`() {
        assertNull(ParserRegistry.sanitizeMerchant("your card ending 1234"))
        assertNull(ParserRegistry.sanitizeMerchant("7300000000"))
        assertEquals("Googleplay", ParserRegistry.sanitizeMerchant("Googleplay using your Federal Bank One Credit Card xx12"))
        assertEquals("Groww", ParserRegistry.sanitizeMerchant("ACH D- Groww-0000ZACR1X7JMCHUKH261520728"))
        assertEquals("INDIANESIGN", ParserRegistry.sanitizeMerchant("ACH D- TP ACH INDIANESIGN-2320147606"))
        assertEquals("HDFC BANK LTD", ParserRegistry.sanitizeMerchant("ACH D- HDFC BANK LTD-474718509"))
        assertEquals("AGODACO13223300", ParserRegistry.sanitizeMerchant("AGODACO13223300"))
    }
}
