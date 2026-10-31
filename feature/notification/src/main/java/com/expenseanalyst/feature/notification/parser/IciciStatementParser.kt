package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * Parses ICICI Bank credit card statement SMS.
 *
 * Sample (real layout, Oct 2026; digits and e-mail changed):
 *   "ICICI Bank Credit Card XX1234 Statement is sent to ab******12@gmail.com. Total of Rs 1,22,099.00
 *    or minimum of Rs 6,105.00 is due by 17-NOV-26."
 * None of "total due" / "amount due" appears, so GenericStatementParser missed it and the transaction
 * parsers saved the statement total as a second expense next to the purchase it bills.
 */
class IciciStatementParser : BillStatementParser {

    override val bankName = "ICICI Bank"

    private val iciciPattern = Regex("""(?i)icici""")
    private val statementPattern = Regex("""(?i)\bstatement\b""")
    private val totalPattern = Regex("""(?i)\btotal\s+of\s+(?:rs\.?|inr)\s*([\d,]+(?:\.\d+)?)""")
    private val minimumPattern = Regex("""(?i)\bminimum\s+of\s+(?:rs\.?|inr)\s*([\d,]+(?:\.\d+)?)""")
    private val dueDatePattern = Regex("""(?i)\bdue\s+by\s+(\d{1,2}-[A-Za-z]{3}-\d{2,4})""")
    private val cardPattern = Regex("""(?i)\bcard\s+[xX*]+(\d{4})""")

    override fun canParse(sender: String, body: String): Boolean =
        (iciciPattern.containsMatchIn(sender) || iciciPattern.containsMatchIn(body)) &&
            statementPattern.containsMatchIn(body) && totalPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val total = totalPattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null
        return ParsedBillStatement(
            billerName = bankName,
            totalDue = total,
            minimumDue = minimumPattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull(),
            currencyCode = "INR",
            dueDateMillis = dueDatePattern.find(body)?.groupValues?.get(1)?.let(::parseDate),
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = cardPattern.find(body)?.groupValues?.get(1),
            bankName = bankName
        )
    }

    private fun parseDate(text: String): Long? {
        for (pattern in listOf("dd-MMM-yy", "d-MMM-yy", "dd-MMM-yyyy", "d-MMM-yyyy")) {
            val formatter = DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
            runCatching {
                return LocalDate.parse(text.trim(), formatter).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }
        }
        return null
    }
}
