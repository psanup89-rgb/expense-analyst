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
 *   "Payment of Rs 4,615.92 has been received on your ICICI Bank Credit Card XX9008 through Bharat Bill Payment System"
 */
class IciciParser : TransactionParser {

    override val bankName = "ICICI Bank"

    private val senderPattern = Regex("""(?i)icici""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    // Matches: "Acct XX1234", "a/c XX1234", "Credit Card XX9008", "Card XX9008"
    private val accountPattern = Regex("""(?i)(?:acct|a/c|account|card)\s*(?:no\.?)?\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref\s*(?:no\.?)?\s*:?\s*(\d+)""")
    private val infoPattern = Regex("""(?i)info:\s*(?:upi/\d+/)?([^.]+)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Detect payment confirmations: "Payment of Rs X has been received on your Credit Card"
        val isPayment = Regex("""(?i)\bpayment\b.*\breceived\b.*\b(?:card|credit)\b""").containsMatchIn(body)
        val isDebit = !isPayment && Regex("""(?i)\bdebited\b""").containsMatchIn(body)
        val isCredit = !isPayment && Regex("""(?i)\b(?:credited|received)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit && !isPayment) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = infoPattern.find(body)?.groupValues?.get(1)?.trim()
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
        )
    }
}
