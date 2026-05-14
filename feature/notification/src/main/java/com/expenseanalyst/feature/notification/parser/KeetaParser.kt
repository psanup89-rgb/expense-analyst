package com.expenseanalyst.feature.notification.parser

/**
 * Keeta (food delivery, Saudi Arabia) SMS parser.
 * Sender IDs: Keeta, KEETA. Some Keeta SMS are sent from a generic short-code with
 * "[Keeta]" prefixed in the body, so the body is also matched against the sender pattern.
 *
 * Sample (refund):
 *   "SAR 31.83 refunded to your payment method on 17 Apr 2026 at 12:34."
 * Sample (order cancellation refund):
 *   "[Keeta]The order (ending No: 9434) was canceled for a stock shortage.
 *    SAR 40.25 will return to the original way in 1-14 workdays."
 * Sample (order charge — possible future format):
 *   "SAR 31.83 charged for your Keeta order on 17 Apr 2026."
 */
class KeetaParser : TransactionParser {

    override val bankName = "Keeta"

    private val senderPattern = Regex("""(?i)\bkeeta\b""")

    // "SAR 31.83 refunded" or "SAR 31.83 charged"
    private val amountPattern = Regex(
        """(?i)(?:(INR|SAR|AED|USD|EUR|GBP)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(INR|SAR|AED|USD|EUR|GBP))"""
    )
    // Refund / order cancellation indicators — all map to CREDIT
    private val refundPattern = Regex(
        """(?i)\brefund(?:ed)?\b|\bcancell?ed\b|\bwill\s+return\b|\breturned\b"""
    )
    private val chargePattern = Regex("""(?i)\b(?:charged|debited|paid)\b""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) || senderPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isRefund = refundPattern.containsMatchIn(body)
        val isCharge = !isRefund && chargePattern.containsMatchIn(body)
        if (!isRefund && !isCharge) return null

        val match = amountPattern.find(body) ?: return null
        val currencyCode = (match.groupValues[1].takeIf { it.isNotBlank() }
            ?: match.groupValues[4].takeIf { it.isNotBlank() }) ?: "SAR"
        val amount = (match.groupValues[2].takeIf { it.isNotBlank() }
            ?: match.groupValues[3].takeIf { it.isNotBlank() })
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        return ParsedTransaction(
            amount = amount,
            currencyCode = currencyCode,
            type = if (isRefund) TransactionDirection.CREDIT else TransactionDirection.DEBIT,
            merchant = "Keeta",
            accountLast4 = null,
            referenceNumber = null,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
