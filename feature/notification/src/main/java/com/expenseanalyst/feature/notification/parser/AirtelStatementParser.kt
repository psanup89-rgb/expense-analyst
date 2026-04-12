package com.expenseanalyst.feature.notification.parser

/**
 * Airtel bill parser.
 * Sender IDs: AD-AIRTEL, BW-AIRTEL, AIRTEL, etc.
 *
 * Handles two formats:
 * 1. Bill payment reminder: "Your Airtel Wi-Fi Account ... bill of Rs.X is pending. (Please ignore if already paid)"
 * 2. Bill generation: "Bill for your Airtel Wi-Fi ... has been generated. Amount to be paid: Rs X"
 */
class AirtelStatementParser : BillStatementParser {

    override val bankName = "Airtel"

    private val senderPattern = Regex("""(?i)airtel""")
    // Matches reminder format OR bill generation format ("Bill for your Airtel")
    private val bodyFingerprint = Regex("""(?i)(?:bill\s+(?:of|payment\s+reminder)|airtel.{0,40}bill|bill\s+for\s+your\s+airtel)""")

    // Prefer "Amount to be paid" / "total due" over first raw Rs.X match
    private val totalDuePattern = Regex("""(?i)(?:amount\s+to\s+be\s+paid|total\s+(?:amount\s+)?due|amount\s+due)\s*[:\-]?\s*(?:rs\.?|inr)?\s*([\d,]+\.?\d*)""")
    private val fallbackAmountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    // "Airtel Wi-Fi", "Airtel Postpaid", "Airtel Broadband", etc.
    private val servicePattern = Regex("""(?i)airtel\s+(wi[-\s]?fi|postpaid|prepaid|broadband|fiber|fibre|dth|mobile|xstream)""")
    // "Due Date: 25-Apr-2026" or "Due Date: 25 Apr 2026"
    private val dueDatePattern = Regex("""(?i)due\s+date\s*[:\-]?\s*(\d{1,2}[-/\s]\w{3,9}[-/\s]\d{2,4})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) && bodyFingerprint.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val amount = (totalDuePattern.find(body)?.groupValues?.get(1)
            ?: fallbackAmountPattern.find(body)?.groupValues?.get(1))
            ?.replace(",", "")?.toDoubleOrNull()

        val billerName = servicePattern.find(body)?.let {
            "Airtel ${it.groupValues[1].trim()}"
        } ?: "Airtel"

        val dueDateMillis = dueDatePattern.find(body)?.groupValues?.get(1)?.let { parseDate(it) }

        return ParsedBillStatement(
            billerName = billerName,
            totalDue = amount,
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
        val formats = listOf(
            "dd-MMM-yyyy", "d-MMM-yyyy", "dd MMM yyyy", "d MMM yyyy",
            "dd/MMM/yyyy", "d/MMM/yyyy"
        )
        for (fmt in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
                sdf.isLenient = false
                return sdf.parse(text.trim())?.time
            } catch (_: Exception) { }
        }
        return null
    }
}
