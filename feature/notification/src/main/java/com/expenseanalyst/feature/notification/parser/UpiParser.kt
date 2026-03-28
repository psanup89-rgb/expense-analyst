package com.expenseanalyst.feature.notification.parser

/**
 * Generic UPI / Google Pay / PhonePe / Paytm notification parser.
 * Catches common UPI notification formats from any sender.
 *
 * Sample (GPay sent):
 *   "You paid ₹200 to Swiggy via Google Pay. UPI Ref: 123456789012"
 * Sample (PhonePe):
 *   "₹150.00 paid to Zomato via PhonePe. Transaction ID: T2501011234"
 * Sample (Paytm):
 *   "Rs. 80 paid to Auto Rickshaw via Paytm UPI. Txn ID: PAY2501011234"
 */
class UpiParser : TransactionParser {

    override val bankName = "UPI"

    private val senderPattern = Regex("""(?i)(?:gpay|googlepay|phonepe|paytm|upi|bharatpe)""")
    private val amountPattern = Regex("""(?i)(?:₹|rs\.?|inr)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:rs\.?|inr)""")
    private val refPattern = Regex("""(?i)(?:upi\s*ref:?\s*|transaction\s*id:?\s*|txn\s*id:?\s*|txn:?\s*)(\w+)""")
    private val paidToPattern = Regex("""(?i)(?:paid\s+to|you\s+paid)\s+([A-Za-z0-9 _\-&.@]+?)(?:\s*(?:via|using|\.|\s*$))""")
    private val receivedFromPattern = Regex("""(?i)received\s+(?:from|by)\s+([A-Za-z0-9 _\-&.@]+?)(?:\s*(?:via|using|\.|\s*$))""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) ||
            Regex("""(?i)\bupi\b""").containsMatchIn(body) && amountPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\b(?:paid|debited|sent|deducted)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\b(?:received|credited)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amountMatch = amountPattern.find(body)
        val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = if (isDebit) paidToPattern.find(body)?.groupValues?.get(1)?.trim()
        else receivedFromPattern.find(body)?.groupValues?.get(1)?.trim()

        return ParsedTransaction(
            amount = amount,
            currencyCode = "INR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant?.takeIf { it.isNotBlank() && it.length < 60 },
            accountLast4 = null,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body) ?: "UPI"
        )
    }
}
