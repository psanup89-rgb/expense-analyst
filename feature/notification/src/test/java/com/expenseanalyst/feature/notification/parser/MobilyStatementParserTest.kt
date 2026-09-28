package com.expenseanalyst.feature.notification.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Mobily's real wordings (owner's inbox, Sep 2026) with names and numbers changed. */
class MobilyStatementParserTest {

    private val parser = MobilyStatementParser()

    private val billIssued = "Hello NAME,\n\nWe hope you enjoyed our services!\n\nYour Mobily Home Fiber bill has been issued " +
        "on package eLife Connect Postpaid for the month of 08-2026 with amount of 250.00 SAR VAT Inclusive.\n" +
        "Total due amount is SAR 287.49 VAT inclusive.\n\nPlease use account number 1000000000000000  to pay before " +
        "25-09-2026 through payment channels or the direct link:http://mobily.im/X"
    private val oldBillIssued = "Dear NAME,\nWe hope you are enjoying all the benefits of your Mobily Fiber services.\n" +
        "Kindly note that Mobily Fiber service bill has been issued for your account number 1000000000 with an amount " +
        "of SAR 287.49 please use this number to settle your payments."
    private val kindlyRemind = "Hello NAME,\n\nThis is to kindly remind you that your Mobily Home Fiber bill has been issued " +
        "for the month of 08-2026\n\nThe total amount due is 287.49 SAR VAT Inclusive.\n\nPlease use account number " +
        "1000000000000000 to pay before 25-09-2026 through payment channels"
    private val willSuspend = "Hello NAME,\n\nWe regret to inform you that your Mobily Home Fiber will be suspended in 48 hours " +
        "for not paying the total due amount of SAR 287.49 VAT Inclusive for month 08-2026\n\nPlease pay using the account number"
    private val deactivated = "Dear NAME,\nKindly note that your Mobily Fiber service will be temporarily deactivated within 3 days, " +
        "as the service bill for June with an outstanding amount of SAR 287.49 is not settled yet."
    private val suspended = "Hello NAME,\n\nWe regret to inform you that your Mobily Home Fiber service has been temporarily " +
        "suspended until the full payment is received.\n\nTotal due amount of SAR 37.49 VAT Inclusive for the month of 08-2026"

    @Test
    fun `bill issued gives the amount to pay and the due date`() {
        val s = parser.parse("Mobily", billIssued)
        assertNotNull(s)
        assertEquals("Mobily", s!!.billerName)
        assertEquals(287.49, s.totalDue!!, 0.001)   // the total due, not the package amount
        assertEquals("SAR", s.currencyCode)
        assertNotNull(s.dueDateMillis)
        assertNull(s.reference)                      // the account number is never kept
        assertNull(s.accountLast4)
    }

    @Test
    fun `every reminder wording parses to the same bill amount`() {
        for (body in listOf(oldBillIssued, kindlyRemind, willSuspend, deactivated)) {
            assert(parser.canParse("Mobily", body)) { body.take(40) }
            assertEquals(287.49, parser.parse("Mobily", body)!!.totalDue!!, 0.001, body.take(40))
        }
        assertEquals(37.49, parser.parse("Mobily", suspended)!!.totalDue!!, 0.001)
    }

    @Test
    fun `payment thank-yous, OTPs and offers are not bills`() {
        listOf(
            "Hello NAME,\n\nThank you for your payment of 250 SAR for your Mobily Home Fiber 500 Mbps postpaid account " +
                "number 1000 processed on Sun Sep 27\n\nPlease note the remaining due amount is\n37.49 SAR.",
            "Dear NAME,\n\nYou have requested account bill payment number 1000\n\nTo continue, please use this OTP confirmation code:\n1234",
            "Don't miss it! Enjoy 500 national minutes to all networks valid for 1 month for only SAR 30"
        ).forEach { assertFalse(parser.canParse("Mobily", it), it.take(40)) }
    }

    @Test
    fun `the registry routes Mobily bills here, and never as a transaction`() {
        assertEquals("Mobily", BillStatementParserRegistry.parse("Mobily", billIssued)?.billerName)
        assertEquals("Mobily", BillStatementParserRegistry.parse("Mobily", suspended)?.billerName)
        // the suspension notice was saved as INCOME before
        assertNull(ParserRegistry.parse("Mobily", suspended))
        assertNull(ParserRegistry.parse("Mobily", willSuspend))
        assertNull(ParserRegistry.parse("Mobily", billIssued))
    }

    @Test
    fun `an Al Rajhi foreign purchase with a total due line is still a transaction`() {
        val body = "Online Purchase\nCard:1234 ;Visa\nAmount:20USD(75.00 SAR)\nAt: ANTHROPIC\nFee &VAT: 2.00 SAR\n" +
            "Exchange rate~ 3.75\nTotal due amount:77.00 SAR\nCountry:USA\nBalance:1000.00 SAR\n1/9/26 10:00"
        assertNotNull(ParserRegistry.parse("AlRajhiBank", body))
    }
}
