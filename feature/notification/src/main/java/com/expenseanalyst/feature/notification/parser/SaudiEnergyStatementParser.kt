package com.expenseanalyst.feature.notification.parser

/**
 * Parses Saudi Energy (SE) electricity bill SMS into bill entries.
 * Sender IDs typically: "SE", "SaudiEnergy", or similar.
 *
 * Sample:
 *   "Dear ANOOP,
 *    Your bill for account 30143319970 has been issued with amount of 149.43 SAR.
 *    You can view and pay your bills via SE app using the following link:
 *    https://www.se.com.sa/app
 *    You can pay your bill using stc Qitaf points or Al Rajhi mokafaa points through the app."
 */
class SaudiEnergyStatementParser : BillStatementParser {

    override val bankName = "Saudi Energy"

    private val bodyFingerprint = Regex("""(?i)se\.com\.sa|your\s+bill\s+for\s+account\b""")
    private val amountPattern = Regex("""(?i)amount\s+of\s+([\d,]+\.?\d*)\s*SAR""")
    private val accountPattern = Regex("""(?i)(?:for\s+)?account\s+(\d+)""")

    override fun canParse(sender: String, body: String): Boolean =
        bodyFingerprint.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val amount = amountPattern.find(body)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountNumber = accountPattern.find(body)?.groupValues?.get(1)?.trim()

        return ParsedBillStatement(
            billerName = "Saudi Energy",
            totalDue = amount,
            minimumDue = amount,
            currencyCode = "SAR",
            dueDateMillis = null,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = accountNumber?.takeLast(4),
            bankName = bankName,
            reference = accountNumber,
            rawBody = body
        )
    }
}
