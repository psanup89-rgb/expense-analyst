package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses Emirates NBD credit card statement SMS messages.
 *
 * Samples:
 *   "Your Emirates NBD Credit Card ending 4388 statement is ready.
 *    Total Due: AED 5,000.00. Minimum Due: AED 250.00.
 *    Payment Due Date: 15 Apr 2026."
 *
 *   "ENBD: Statement for Card XXXX4388 - Mar 2026.
 *    Total Amt Due: AED 3,200. Min Due: AED 320. Due: 10-Apr-2026"
 */
class EmiratesNbdStatementParser : BillStatementParser {

    override val bankName = "Emirates NBD"

    private val senderPattern = Regex("""(?i)(?:emirates\s*nbd|enbd|emiratesnbd)""")
    private val bodyFingerprintPattern = Regex(
        """(?i)(?:total\s+(?:amt\s+)?due|minimum\s+(?:amt\s+)?due|min\s+due).*(?:aed|sar|usd)""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val totalDuePattern = Regex("""(?i)total\s+(?:amt\s+)?due\s*[:\-]\s*(?:aed|sar|usd|inr)?\s*([\d,]+\.?\d*)""")
    private val minDuePattern = Regex("""(?i)min(?:imum)?\s+(?:amt\s+)?(?:due|payment)\s*[:\-]\s*(?:aed|sar|usd|inr)?\s*([\d,]+\.?\d*)""")
    private val dueDatePattern = Regex("""(?i)(?:payment\s+)?due\s+(?:date|by)\s*[:\-]\s*(.{5,20}?)(?:\.|$|\n)""")
    private val accountPattern = Regex("""(?i)(?:card|account)\s*(?:ending|no\.?|number|xxxx)?\s*[xX*]*(\d{4})""")
    private val currencyPattern = Regex("""(?i)\b(AED|SAR|USD|GBP|EUR)\b""")

    override fun canParse(sender: String, body: String): Boolean =
        (senderPattern.containsMatchIn(sender) || body.contains("emirates nbd", ignoreCase = true) ||
            body.contains("enbd", ignoreCase = true)) &&
            (body.contains("statement", ignoreCase = true) || bodyFingerprintPattern.containsMatchIn(body))

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val totalDue = totalDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val minimumDue = minDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        if (totalDue == null && minimumDue == null) return null

        val currency = currencyPattern.find(body)?.groupValues?.get(1) ?: "AED"
        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val dueDateStr = dueDatePattern.find(body)?.groupValues?.get(1)?.trim()
        val dueDateMillis = dueDateStr?.let { parseDate(it) }

        return ParsedBillStatement(
            billerName = bankName,
            totalDue = totalDue,
            minimumDue = minimumDue,
            currencyCode = currency,
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
            "MMM dd, yyyy", "MMM d, yyyy",
            "dd/MM/yyyy", "d/M/yyyy",
            "dd-MM-yyyy", "d-M-yyyy"
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
