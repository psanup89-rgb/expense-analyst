package com.expenseanalyst.feature.notification.parser

/**
 * OneCard (Federal Bank) credit card SMS parser.
 * Sender IDs: *-OneCrd, *-OneCrd-S, *-OneCrd-T, CPOneCrd, etc.
 *
 * Sample (spend):
 *   "You've paid AED 24.99 at Noon with your Federal One Credit Card ending in XX3550 and earned reward points!"
 * Sample (payment received):
 *   "Hola! that was sweet. We have received payment against your OneCard for Rs. 13,388.20 on 25 Mar 2026."
 * Sample (refund):
 *   "Hi, We have received a refund of Rs. 794.48 from ZOMATO on your Federal One Credit Card."
 */
class OneCardParser : TransactionParser {

    override val bankName = "OneCard"

    private val senderPattern = Regex("""(?i)onecrd""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr|aed|usd|eur|gbp)\s*([\d,]+\.?\d*)""")
    // "Federal One Credit Card ending in XX3550" or "Credit Card xx50"
    private val accountPattern = Regex("""(?i)(?:card|Card)\s*(?:ending\s*(?:in)?)?\s*[xX*]+(\d{2,4})""")
    // "paid AED 24.99 at Noon with" or "at Zomato using"
    private val atMerchantPattern = Regex("""(?i)\bat\s+([A-Za-z0-9][A-Za-z0-9 _\-&.]*?)\s+(?:with|using)\b""")
    // "refund of Rs. X from ZOMATO on"
    private val refundFromPattern = Regex("""(?i)refund\s+of\s+.*?\bfrom\s+([A-Za-z0-9][A-Za-z0-9 _\-&.]*?)\s+on\b""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Spend: "You've paid X at MERCHANT"
        val isPaid = Regex("""(?i)\bpaid\b.*\bat\b""").containsMatchIn(body)
        // Payment to card: "received payment against your OneCard"
        val isPayment = Regex("""(?i)\breceived\s+payment\b""").containsMatchIn(body)
        // Refund: "received a refund"
        val isRefund = Regex("""(?i)\brefund\b""").containsMatchIn(body)

        if (!isPaid && !isPayment && !isRefund) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        // Detect currency
        val bodyUpper = body.uppercase()
        val currencyCode = when {
            bodyUpper.contains("AED") -> "AED"
            bodyUpper.contains("USD") -> "USD"
            bodyUpper.contains("EUR") -> "EUR"
            bodyUpper.contains("GBP") -> "GBP"
            else -> "INR"
        }

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val merchant = (atMerchantPattern.find(body)?.groupValues?.get(1)
            ?: refundFromPattern.find(body)?.groupValues?.get(1))
            ?.trim()?.takeIf { it.isNotBlank() && it.length < 60 }

        val direction = when {
            isPaid -> TransactionDirection.DEBIT
            isRefund -> TransactionDirection.CREDIT
            isPayment -> TransactionDirection.PAYMENT
            else -> TransactionDirection.DEBIT
        }

        return ParsedTransaction(
            amount = amount,
            currencyCode = currencyCode,
            type = direction,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = null,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body) ?: "CREDIT_CARD"
        )
    }
}
