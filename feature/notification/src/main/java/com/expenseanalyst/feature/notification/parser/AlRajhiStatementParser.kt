package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses Al Rajhi Bank credit card statement SMS messages.
 *
 * Samples:
 *   "Al Rajhi Bank - Credit Card Statement
 *    Card: XXXX7573. Total Due: SAR 2,400.50. Min: SAR 200. Due: 10/04/2026"
 *
 *   "AlRajhi: Your CC ending 7573 statement for Mar 2026 is ready.
 *    Total Amount Due: SAR 3,150. Minimum Due: SAR 315. Payment Due: 15 Apr 2026"
 */
class AlRajhiStatementParser : BillStatementParser {

    override val bankName = "Al Rajhi Bank"

    private val senderPattern = Regex("""(?i)(?:alrajhi|al.rajhi|rajhi|74100)""")

    private val totalDuePattern = Regex("""(?i)total\s+(?:amount\s+)?due\s*[:\-]?\s*(?:sar|sr|ر\.س)?\s*([\d,]+\.?\d*)""")
    private val minDuePattern = Regex("""(?i)min(?:imum)?\s+(?:due|payment|amount)?\s*[:\-]?\s*(?:sar|sr|ر\.س)?\s*([\d,]+\.?\d*)""")
    private val dueDatePattern = Regex("""(?i)(?:payment\s+)?due\s*(?:date|by)?\s*[:\-]\s*(.{5,20}?)(?:\.|$|\n)""")
    private val accountPattern = Regex("""(?i)(?:card|cc|account)\s*(?:ending|no\.?|xxxx)?\s*[xX*]*(\d{4})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) &&
            body.contains("statement", ignoreCase = true) &&
            (body.contains("total", ignoreCase = true) || body.contains("due", ignoreCase = true))

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val totalDue = totalDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val minimumDue = minDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        if (totalDue == null && minimumDue == null) return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val dueDateStr = dueDatePattern.find(body)?.groupValues?.get(1)?.trim()
        val dueDateMillis = dueDateStr?.let { parseDate(it) }

        return ParsedBillStatement(
            billerName = bankName,
            totalDue = totalDue,
            minimumDue = minimumDue,
            currencyCode = "SAR",
            dueDateMillis = dueDateMillis,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = accountLast4,
            bankName = bankName
        )
    }

    private fun parseDate(text: String): Long? {
        val formats = listOf(
            "dd MMM yyyy", "d MMM yyyy", "dd-MMM-yyyy", "d-MMM-yyyy",
            "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy"
        )
        for (pattern in formats) {
            try {
                val date = LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
                return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) { }
        }
        return null
    }
}
