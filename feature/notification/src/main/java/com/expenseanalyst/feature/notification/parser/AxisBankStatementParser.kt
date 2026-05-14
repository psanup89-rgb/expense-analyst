package com.expenseanalyst.feature.notification.parser

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses Axis Bank credit card bill-due SMS messages.
 * Sender IDs: AXISBK, AXISBN, AD-AXISBK
 *
 * Samples:
 *   "Payment of INR 8523.16 for Axis Bank Credit Card no. XX3745 is due on
 *    04-04-26 with minimum amount due of INR 8523.16. Ignore if paid."
 *   "INR 10747.2 is due for payment on 10-05-26 towards Axis Bank CC no. XX4502.
 *    INR 215 will be debited from Axis Bank A/c no. XX0426 via auto debit."
 */
class AxisBankStatementParser : BillStatementParser {

    override val bankName = "Axis Bank"

    private val senderPattern = Regex("""(?i)axis""")
    private val bodyFingerprint = Regex(
        """(?i)credit\s+card.*?due\s+on|payment\s+of\s+(?:INR|Rs).*?due\s+on|is\s+due\s+for\s+payment\s+on.*?(?:axis|cc\s+no)""",
        RegexOption.DOT_MATCHES_ALL
    )

    // "Payment of INR 8523.16" OR "INR 10747.2 is due for payment on …" — first amount is total due
    private val totalDuePattern = Regex(
        """(?i)Payment\s+of\s+(?:INR|Rs\.?)\s*([\d,]+\.?\d*)|(?:INR|Rs\.?)\s*([\d,]+\.?\d*)\s+is\s+due\s+for\s+payment"""
    )
    // "minimum amount due of INR 8523.16"
    private val minDuePattern = Regex("""(?i)minimum\s+amount\s+due\s+of\s+(?:INR|Rs\.?)\s*([\d,]+\.?\d*)""")
    // "due on 04-04-26" or "due for payment on 10-05-26"
    private val dueDatePattern = Regex("""(?i)due\s+(?:for\s+payment\s+)?on\s+(\d{1,2}-\d{1,2}-\d{2,4})""")
    // "XX3745" — prefer the CC card last-4 over the auto-debit account last-4
    private val ccAccountPattern = Regex("""(?i)(?:credit\s+card|cc)\s+no\.?\s*XX\s*(\d{4})""")
    private val accountPattern = Regex("""(?i)XX\s*(\d{4})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) && bodyFingerprint.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val totalMatch = totalDuePattern.find(body)
        val totalDue = totalMatch?.let { m ->
            (m.groupValues[1].takeIf { it.isNotBlank() } ?: m.groupValues[2].takeIf { it.isNotBlank() })
        }?.replace(",", "")?.toDoubleOrNull()
        val minimumDue = minDuePattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        if (totalDue == null && minimumDue == null) return null

        val accountLast4 = ccAccountPattern.find(body)?.groupValues?.get(1)
            ?: accountPattern.find(body)?.groupValues?.get(1)
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
            "dd-MM-yy", "d-M-yy",
            "dd-MM-yyyy", "d-M-yyyy",
            "dd-MMM-yy", "dd-MMM-yyyy"
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
