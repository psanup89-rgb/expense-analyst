package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Mobily (Saudi telecom) bills and their reminders. Wording from the owner's real inbox
 * (Sep 2026, numbers changed):
 *
 * Bill issued:
 *   "Your Mobily Home Fiber bill has been issued on package eLife Connect Postpaid for the month
 *    of 08-2026 with amount of 287.49 SAR VAT Inclusive. Total due amount is SAR 287.49 VAT
 *    inclusive. Please use account number … to pay before 25-09-2026 …"
 *   "Kindly note that Mobily Fiber service bill has been issued for your account number … with an
 *    amount of SAR 287.49 …"
 * Reminders (same bill, re-sent):
 *   "This is to kindly remind you that your Mobily Home Fiber bill has been issued for the month
 *    of 08-2026. The total amount due is 287.49 SAR VAT Inclusive. … pay before 25-09-2026"
 *   "… remind you that your Mobily Fiber service bill has been issued since the beginning of this
 *    month … with an outstanding amount of SAR 287.49"
 *   "… your Mobily Home Fiber will be suspended in 48 hours for not paying the total due amount of
 *    SAR 287.49 VAT Inclusive for month 08-2026"
 *   "… service will be temporarily deactivated within 3 days, as the service bill for June with an
 *    outstanding amount of SAR 287.49 is not settled yet"
 *   "… service has been temporarily suspended until the full payment is received. Total due
 *    amount of SAR 37.49 VAT Inclusive …"
 *
 * All of these parse to the same statement; [BillStatementManager] decides whether it is a new
 * bill or a reminder of one already saved (same biller + amount). Payment thank-yous ("remaining
 * due amount is …"), OTPs, offers and the Arabic copies are deliberately not matched. The account
 * number is never kept — a bill's reference is shown in the app.
 */
class MobilyStatementParser : BillStatementParser {

    override val bankName = "Mobily"

    private val mobily = Regex("""(?i)\bmobily\b""")
    private val billNotice = Regex(
        """(?i)bill\s+has\s+been\s+issued|will\s+be\s+(?:temporarily\s+)?(?:suspended|deactivated)""" +
            """|has\s+been\s+temporarily\s+suspended"""
    )

    // Most specific first: the amount to pay, then the bill amount
    private val amountPatterns = listOf(
        Regex("""(?i)total\s+due\s+amount\s+(?:is|of)\s+SAR\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)total\s+amount\s+due\s+is\s+([\d,]+(?:\.\d+)?)\s*SAR"""),
        Regex("""(?i)outstanding\s+amount\s+of\s+SAR\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)amount\s+of\s+SAR\s*([\d,]+(?:\.\d+)?)"""),
        Regex("""(?i)amount\s+of\s+([\d,]+(?:\.\d+)?)\s*SAR""")
    )
    private val payBeforePattern = Regex("""(?i)pay\s+before\s+(\d{1,2}-\d{1,2}-\d{4})""")

    override fun canParse(sender: String, body: String): Boolean =
        (mobily.containsMatchIn(sender) || mobily.containsMatchIn(body)) && billNotice.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val amount = amountPatterns.firstNotNullOfOrNull { p ->
            p.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        } ?: return null
        val due = payBeforePattern.find(body)?.groupValues?.get(1)?.let { text ->
            runCatching {
                LocalDate.parse(text, DateTimeFormatter.ofPattern("d-M-yyyy"))
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        }
        return ParsedBillStatement(
            billerName = "Mobily",
            totalDue = amount,
            minimumDue = amount,
            currencyCode = "SAR",
            dueDateMillis = due,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = null,
            bankName = bankName,
            rawBody = body,
            reference = null
        )
    }
}
