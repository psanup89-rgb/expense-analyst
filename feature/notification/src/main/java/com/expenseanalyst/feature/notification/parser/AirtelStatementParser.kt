package com.expenseanalyst.feature.notification.parser

/**
 * Airtel bill payment reminder parser.
 * Sender IDs: AD-AIRTEL, BW-AIRTEL, AIRTEL, etc.
 *
 * Sample (Wi-Fi bill reminder):
 *   "Bill payment reminder: Your Airtel Wi-Fi Account 20025035912 bill of Rs.1059.64
 *    is pending. Pay your bill immediately to keep your Wi-Fi service running smoothly.
 *    (Please ignore if already paid)"
 */
class AirtelStatementParser : BillStatementParser {

    override val bankName = "Airtel"

    private val senderPattern = Regex("""(?i)airtel""")
    // Matches "bill of Rs.X", "bill payment reminder", "Airtel ... bill"
    private val bodyFingerprint = Regex("""(?i)(?:bill\s+(?:of|payment\s+reminder)|airtel.{0,40}bill)""")

    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    // "Airtel Wi-Fi", "Airtel Postpaid", "Airtel Broadband", etc.
    private val servicePattern = Regex("""(?i)airtel\s+(wi[-\s]?fi|postpaid|prepaid|broadband|fiber|fibre|dth|mobile|xstream)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) && bodyFingerprint.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val amount = amountPattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        val billerName = servicePattern.find(body)?.let {
            "Airtel ${it.groupValues[1].trim()}"
        } ?: "Airtel"

        return ParsedBillStatement(
            billerName = billerName,
            totalDue = amount,
            minimumDue = null,
            currencyCode = "INR",
            dueDateMillis = null,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = null,
            bankName = bankName
        )
    }
}
