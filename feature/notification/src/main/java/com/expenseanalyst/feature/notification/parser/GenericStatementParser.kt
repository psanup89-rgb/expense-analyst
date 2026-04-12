package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Generic fallback parser for bill statement SMS from any bank.
 * Only triggers when both "statement" AND a "total due / amount due" phrase are present,
 * preventing false positives on ordinary transaction messages.
 */
class GenericStatementParser : BillStatementParser {

    override val bankName = "Unknown Bank"

    private val statementKeyword = Regex("""(?i)\b(?:statement|bill\s+generated|bill\s+ready)\b""")
    private val dueKeyword = Regex("""(?i)\b(?:total\s+(?:amt\s+)?due|amount\s+due|outstanding\s+(?:amount|balance)|total\s+outstanding|minimum\s+(?:amount\s+)?due)\b""")
    // Bill-due reminder without "statement" keyword: "X is due on date", "bill of Rs.X is pending", "bill payment reminder"
    private val billDueReminder = Regex("""(?i)(?:is\s+due\s+on\s+\d|bill\s+(?:of|amount).{0,40}(?:pending|is\s+due)|bill\s+payment\s+reminder|payment\s+reminder)""")
    private val hasAmount = Regex("""(?i)(?:rs\.?|inr|sar|aed|usd|gbp|eur)\s*[\d,]+""")

    private val totalDuePattern = Regex("""(?i)(?:total\s+(?:amt\s+)?due|amount\s+due|outstanding\s+(?:amount|balance)|payment\s+of)\s*[:\-]?\s*(?:rs\.?|inr|sar|aed|usd|gbp|eur)?\s*([\d,]+\.?\d*)""")
    private val minDuePattern = Regex("""(?i)min(?:imum)?\s+(?:amt\s+|amount\s+)?(?:due|payment)\s*(?:of\s+)?(?:due\s+)?[:\-]?\s*(?:rs\.?|inr|sar|aed|usd|gbp|eur)?\s*([\d,]+\.?\d*)""")
    private val dueDatePattern = Regex("""(?i)(?:payment\s+)?due\s+(?:date|by|on)\s*[:\-]?\s*(.{5,20}?)(?:\.|$|\n)""")
    private val accountPattern = Regex("""(?i)(?:card|account)\s*(?:ending|no\.?|xxxx)?\s*[xX*]*(\d{4})""")
    private val currencyPattern = Regex("""(?i)\b(INR|SAR|AED|USD|GBP|EUR)\b""")

    override fun canParse(sender: String, body: String): Boolean =
        (statementKeyword.containsMatchIn(body) && dueKeyword.containsMatchIn(body)) ||
        (billDueReminder.containsMatchIn(body) && hasAmount.containsMatchIn(body))

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val totalDue = totalDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val minimumDue = minDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        // Fallback: extract any currency amount when specific patterns don't match
        val fallbackAmount = if (totalDue == null && minimumDue == null) {
            Regex("""(?i)(?:rs\.?|inr|sar|aed|usd|gbp|eur)\s*([\d,]+\.?\d*)""")
                .find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        } else null
        if (totalDue == null && minimumDue == null && fallbackAmount == null) return null

        val currency = currencyPattern.find(body)?.groupValues?.get(1) ?: "INR"
        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val dueDateStr = dueDatePattern.find(body)?.groupValues?.get(1)?.trim()
        val dueDateMillis = dueDateStr?.let { parseDate(it) }

        // Derive biller name from sender if it looks like a bank name, else fall back
        val biller = sender.trim()
            .takeIf { it.isNotBlank() && it.all { c -> c.isLetter() || c.isWhitespace() || c == '-' } }
            ?: bankName

        return ParsedBillStatement(
            billerName = biller,
            totalDue = totalDue ?: fallbackAmount,
            minimumDue = minimumDue,
            currencyCode = currency,
            dueDateMillis = dueDateMillis,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = accountLast4,
            bankName = biller
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
