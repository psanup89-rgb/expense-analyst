package com.expenseanalyst.feature.notification.parser

/**
 * ICICI Bank SMS parser.
 * Sender IDs: ICICIB, ICICIBANK, AD-ICICIB, ICICIT
 *
 * Sample (debit):
 *   "ICICI Bank Acct XX1234: Rs 450.00 debited on 01-Jan-25. Info: UPI/123456/Zomato. Avl Bal: Rs 5500.00"
 * Sample (credit):
 *   "ICICI Bank Acct XX1234: Rs 1500.00 credited on 01-Jan-25. Ref 98765432."
 * Sample (payment received):
 *   "Payment of Rs 4,615.92 has been received on your ICICI Bank Credit Card XX1234 through Bharat Bill Payment System"
 * Sample (credit card purchase, real wording Oct 2026):
 *   "Rs 1,22,099.00 spent on ICICI Bank Card XX1234 on 11-Oct-26 at WWWCROMACOM. Avl Lmt: Rs 27,901.00. To dispute, …"
 *   No "debited" anywhere, and GenericParser doesn't treat "spent" as a debit either, so until
 *   the card-spend branch existed every ICICI card purchase was silently dropped.
 */
class IciciParser : TransactionParser {

    override val bankName = "ICICI Bank"

    private val senderPattern = Regex("""(?i)icici""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    // Matches: "Acct XX1234", "a/c XX1234", "Credit Card XX9008", "Card XX9008"
    private val accountPattern = Regex("""(?i)(?:acct|a/c|account|card)\s*(?:no\.?)?\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref\s*(?:no\.?)?\s*:?\s*(\d+)""")
    private val infoPattern = Regex("""(?i)info:\s*(?:upi/\d+/)?([^.]+)""")
    // "Rs X spent on ICICI Bank Card XX1234 on 11-Oct-26 at MERCHANT. Avl Lmt: …"
    private val cardSpendPattern = Regex("""(?i)\bspent\s+(?:on|using)\s+(?:your\s+)?icici\s+bank\s+(?:credit\s+)?card\b""")
    private val spendMerchantPattern = Regex("""(?i)\bon\s+\d{1,2}-[A-Za-z]{3}-\d{2,4}\s+at\s+(.+?)(?:\.\s|\.?$)""")
    // A card with an available limit is a credit card; the generic detector only knows the words.
    private val availableLimitPattern = Regex("""(?i)\bavl\.?\s*lmt\b|\bavailable\s+limit\b""")
    // Online descriptors arrive run together: "WWWCROMACOM" -> "CROMA".
    private val webDescriptorPattern = Regex("""(?i)^www\.?(.+?)\.?(?:co\.in|com|in)$""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Detect payment confirmations: "Payment of Rs X has been received on your Credit Card"
        val isPayment = Regex("""(?i)\bpayment\b.*\breceived\b.*\b(?:card|credit)\b""").containsMatchIn(body)
        val isCardSpend = !isPayment && cardSpendPattern.containsMatchIn(body)
        val isDebit = isCardSpend || (!isPayment && Regex("""(?i)\bdebited\b""").containsMatchIn(body))
        val isCredit = !isPayment && Regex("""(?i)\b(?:credited|received)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit && !isPayment) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = (if (isCardSpend) spendMerchantOf(body) else infoPattern.find(body)?.groupValues?.get(1))
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        val direction = when {
            isPayment -> TransactionDirection.PAYMENT
            isDebit -> TransactionDirection.DEBIT
            else -> TransactionDirection.CREDIT
        }

        return ParsedTransaction(
            amount = amount,
            currencyCode = "INR",
            type = direction,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
                ?: if (availableLimitPattern.containsMatchIn(body)) "CREDIT_CARD" else null
        )
    }

    private fun spendMerchantOf(body: String): String? {
        val raw = spendMerchantPattern.find(body)?.groupValues?.get(1)?.trim() ?: return null
        return webDescriptorPattern.matchEntire(raw)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: raw
    }
}
