package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses HDFC Bank credit card statement SMS messages.
 *
 * Sample:
 *   "HDFC Bank: Your Credit Card no. XX1041 Statement for Mar'26 is ready.
 *    Total Amt Due: Rs.15,423.00. Min Amt Due: Rs.1,542.00. Due Date: 05 Apr 2026"
 */
class HdfcStatementParser : BillStatementParser {

    override val bankName = "HDFC Bank"

    private val senderPattern = Regex("""(?i)hdfc""")

    // HDFC uses "Rs." prefix — handle both "Rs.15,423.00" and "Rs. 15,423.00"
    private val totalDuePattern = Regex("""(?i)total\s+amt\s+due\s*[:\-]?\s*(?:rs\.?\s*)?([\d,]+\.?\d*)""")
    private val totalDuePattern2 = Regex("""(?i)total\s+(?:amount\s+)?due\s*[:\-]?\s*(?:rs\.?|inr)?\s*([\d,]+\.?\d*)""")
    private val minDuePattern = Regex("""(?i)min(?:imum)?\s+amt\s+due\s*[:\-]?\s*(?:rs\.?\s*)?([\d,]+\.?\d*)""")
    private val minDuePattern2 = Regex("""(?i)min(?:imum)?\s+(?:amount\s+)?due\s*[:\-]?\s*(?:rs\.?|inr)?\s*([\d,]+\.?\d*)""")
    private val dueDatePattern = Regex("""(?i)due\s+date\s*[:\-]\s*(.{5,20}?)(?:\.|$|\n)""")
    private val accountPattern = Regex("""(?i)(?:card|account)\s*(?:no\.?|ending|xx)?\s*[xX*]*(\d{4})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) &&
            body.contains("statement", ignoreCase = true) &&
            (body.contains("total amt due", ignoreCase = true) || body.contains("total amount due", ignoreCase = true))

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val totalDue = (totalDuePattern.find(body) ?: totalDuePattern2.find(body))
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val minimumDue = (minDuePattern.find(body) ?: minDuePattern2.find(body))
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
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
        val formats = listOf(
            "dd MMM yyyy", "d MMM yyyy", "dd-MMM-yyyy",
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
