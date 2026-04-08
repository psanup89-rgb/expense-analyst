package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses IDFC FIRST Bank credit card bill-due SMS messages.
 * Sender IDs: *-IDFCFB, *-IDFCFB-S, TMIDFCFB, etc.
 *
 * Sample:
 *   "Your Mayura Credit Card XX6887 bill due by 06 April, 2026
 *    Total Due: INR 5031.37
 *    Min Due: INR 5031.37
 *    Pay: https://idfcfr.in/i20etK
 *    IDFC FIRST Bank"
 */
class IdfcFirstBankStatementParser : BillStatementParser {

    override val bankName = "IDFC FIRST Bank"

    private val senderPattern = Regex("""(?i)(?:idfcfb|idfc\s*first)""")

    // "bill due by 06 April, 2026" — anchored to ensure it's a bill-due message
    private val billDueBodyFingerprint = Regex("""(?i)bill\s+due\s+by""")

    private val totalDuePattern = Regex("""(?i)total\s+due\s*[:\-]?\s*(?:INR|Rs\.?)?\s*([\d,]+\.?\d*)""")
    private val minDuePattern = Regex("""(?i)min(?:imum)?\s+due\s*[:\-]?\s*(?:INR|Rs\.?)?\s*([\d,]+\.?\d*)""")

    // "bill due by 06 April, 2026"
    private val dueDatePattern = Regex("""(?i)bill\s+due\s+by\s+(.{5,25}?)(?:\n|$)""")

    // "XX6887" or "XX 6887"
    private val accountPattern = Regex("""(?i)XX\s*(\d{4})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) && billDueBodyFingerprint.containsMatchIn(body)

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
            currencyCode = "INR",
            dueDateMillis = dueDateMillis,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = accountLast4,
            bankName = bankName
        )
    }

    private fun parseDate(text: String): Long? {
        val cleaned = text.trim().trimEnd(',', '.')
        val formats = listOf(
            "dd MMMM, yyyy", "d MMMM, yyyy",
            "dd MMMM yyyy", "d MMMM yyyy",
            "dd MMM yyyy", "d MMM yyyy",
            "dd MMM, yyyy", "d MMM, yyyy",
            "dd-MMM-yyyy", "dd/MM/yyyy", "d/M/yyyy"
        )
        for (pattern in formats) {
            try {
                val date = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
                return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) { }
        }
        return null
    }
}
