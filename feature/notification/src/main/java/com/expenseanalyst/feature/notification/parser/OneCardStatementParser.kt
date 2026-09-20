package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses OneCard (Federal Bank co-branded) credit card bill-ready messages.
 * Sender IDs are 6-char DLT codes such as FEDONE / OneCrd, so detection is body-based —
 * the wording is specific enough ("One Credit Card bill of ... is ready") not to need the sender.
 *
 * Sample:
 *   "Your Federal Bank  One Credit Card bill of Rs. 20,809.99 is ready. Pay by 07 Oct, 2026
 *    via the OneCard app: https://1crd.in/OneCrd/BillDetails"
 */
class OneCardStatementParser : BillStatementParser {

    override val bankName = "OneCard"

    private val bodyFingerprint = Regex(
        """(?i)One\s+Credit\s+Card\s+bill\s+of\s+(?:rs\.?|inr)\s*[\d,]+.*?is\s+ready""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val totalDuePattern = Regex("""(?i)bill\s+of\s+(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    // "Pay by 07 Oct, 2026" — comma after the month
    private val dueDatePattern = Regex("""(?i)pay\s+by\s+(\d{1,2}\s+[A-Za-z]{3,9},?\s+\d{4})""")

    override fun canParse(sender: String, body: String): Boolean = bodyFingerprint.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val totalDue = totalDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null
        val dueDateMillis = dueDatePattern.find(body)?.groupValues?.get(1)?.let { parseDate(it) }

        return ParsedBillStatement(
            billerName = bankName,
            totalDue = totalDue,
            minimumDue = null,
            currencyCode = "INR",
            dueDateMillis = dueDateMillis,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = null,
            bankName = bankName
        )
    }

    private fun parseDate(text: String): Long? {
        val cleaned = text.replace(",", "").replace(Regex("""\s+"""), " ").trim()
        for (pattern in listOf("dd MMM yyyy", "d MMM yyyy", "d MMMM yyyy")) {
            try {
                val date = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
                return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) { }
        }
        return null
    }
}
