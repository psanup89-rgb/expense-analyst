package com.expenseanalyst.feature.notification.parser

/**
 * Parses Tamara (BNPL) payment reminder SMS messages into bill entries.
 * Sender IDs typically: "Tamara Due", "Tamara", "TamaraPay"
 *
 * Sample:
 *   "Reminder! you have a payment of 516.51 SAR for your Samsung order due in 2 days.
 *    Pay now to improve your credit limits: https://tamara.go.link/aQRmC"
 */
class TamaraStatementParser : BillStatementParser {

    override val bankName = "Tamara"

    private val senderPattern = Regex("""(?i)tamara""")
    private val bodyFingerprint = Regex("""(?i)tamara\.go\.link|tamara""")

    // "payment of 516.51 SAR" or "payment of SAR 516.51"
    private val amountPattern = Regex(
        """(?i)payment\s+of\s+(?:([A-Z]{3})\s*)?([\d,]+\.?\d*)\s*([A-Z]{3})?"""
    )

    // "for your Samsung order" — extract merchant from order description
    private val orderPattern = Regex("""(?i)for\s+(?:your\s+)?(.+?)\s+order""")

    // "due in 2 days" / "due in 1 day"
    private val dueDaysPattern = Regex("""(?i)due\s+in\s+(\d+)\s+days?""")

    override fun canParse(sender: String, body: String): Boolean =
        (senderPattern.containsMatchIn(sender) || bodyFingerprint.containsMatchIn(body)) &&
            amountPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val amountMatch = amountPattern.find(body) ?: return null
        // Currency may be before or after amount
        val currency = (amountMatch.groupValues[1].takeIf { it.isNotBlank() }
            ?: amountMatch.groupValues[3].takeIf { it.isNotBlank() }
            ?: "SAR")
        val amount = amountMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null

        val merchant = orderPattern.find(body)?.groupValues?.get(1)?.trim()
            ?.replaceFirstChar { it.uppercase() }
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        val billerName = if (merchant != null) "Tamara – $merchant" else "Tamara"

        val dueDays = dueDaysPattern.find(body)?.groupValues?.get(1)?.toLongOrNull()
        val dueDateMillis = dueDays?.let {
            System.currentTimeMillis() + it * 24L * 60L * 60L * 1000L
        }

        return ParsedBillStatement(
            billerName = billerName,
            totalDue = amount,
            minimumDue = amount,
            currencyCode = currency,
            dueDateMillis = dueDateMillis,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = null,
            bankName = bankName
        )
    }
}
