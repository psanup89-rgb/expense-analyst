package com.expenseanalyst.feature.notification.parser

/**
 * Handles HDFC Bank debit/credit SMS.
 * Sender IDs: HDFCBK, HDFCBN, AD-HDFCBK, BK-HDFCBK, VD-HDFCBK-T
 *
 * Sample (debit):
 *   "Rs.500.00 debited from a/c XX1234 on 01-01-2025 at Swiggy. Avl bal Rs.12000.00"
 * Sample (credit):
 *   "Rs.1000.00 credited to a/c XX1234 on 01-01-2025. Ref no 12345678"
 * Sample UPI:
 *   "INR 250.00 sent via UPI to merchant@upi. UPI Ref:123456789012. Avl Bal:INR 9800.00"
 * Sample (debit, XX format):
 *   "UPDATE: INR 56,700.00 debited from HDFC Bank XX7823 on 05-MAR-26. Info: ACH D- HDFC BANK LTD"
 * Sample (payment received):
 *   "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 21623.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 1041"
 * Sample (payment alert):
 *   "PAYMENT ALERT! INR 15000.00 deducted from HDFC Bank A/C No 7823 towards INDIAN CLEARING CORP"
 */
class HdfcParser : TransactionParser {

    override val bankName = "HDFC Bank"

    private val senderPattern = Regex("""(?i)hdfc""")
    private val debitPattern = Regex(
        """(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)\s*(?:debited|deducted|sent).*?(?:a/c\s*[xX*]+(\d{3,4}))?.*?(?:at|to|at )\s*([^.]+?)(?:\.|avl|available|upi ref|ref|$)""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val creditPattern = Regex(
        """(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)\s*(?:credited|received).*?(?:a/c\s*[xX*]+(\d{3,4}))?""",
        RegexOption.DOT_MATCHES_ALL
    )
    // Matches: "a/c XX1234", "HDFC Bank XX7823", "A/C No 7823", "card ending 1041", "ENDING WITH 1041"
    private val accountPattern = Regex("""(?i)(?:a/c|HDFC\s+Bank|card|acct)\s*(?:no\.?|ending(?:\s+with)?)?\s*[xX*]*\s*(\d{3,4})""")
    private val refPattern = Regex("""(?i)(?:ref\s*(?:no|num|number)?:?\s*|upi\s*ref:?\s*)(\w+)""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) || senderPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Detect payment confirmations: "PAYMENT OF Rs. X RECEIVED TOWARDS YOUR CREDIT CARD"
        val isPayment = Regex("""(?i)\bpayment\b.*\breceived\b.*\b(?:card|credit)\b""").containsMatchIn(body)
        val isDebit = !isPayment && Regex("""(?i)\b(?:debited|deducted|sent)\b""").containsMatchIn(body)
        val isCredit = !isPayment && Regex("""(?i)\b(?:credited|received)\b""").containsMatchIn(body)

        if (!isDebit && !isCredit && !isPayment) return null

        val amountStr = amountPattern.find(body)?.groupValues?.get(1) ?: return null
        val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = extractMerchant(body)

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

    private fun extractMerchant(body: String): String? {
        val atPattern = Regex("""(?i)\bat\s+([A-Za-z0-9 _\-&.]+?)(?:\s*(?:\.|avl|available|ref|upi|on\s+\d)|\s*$)""")
        val toPattern = Regex("""(?i)\bto\s+([A-Za-z0-9 _\-&.@]+?)(?:\s*(?:\.|avl|available|ref|upi|on\s+\d)|\s*$)""")
        // "Info: ACH D- HDFC BANK" or "Info: UPI/123456/Zomato"
        val infoPattern = Regex("""(?i)Info:\s*(?:UPI/\d+/)?([^.]+?)(?:\.\s|Avl|\s*$)""")
        // "towards INDIAN CLEARING CORP UMRN"
        val towardsPattern = Regex("""(?i)towards\s+([A-Za-z0-9 _\-&.]+?)(?:\s+UMRN|\s*$)""")
        // "For IMPS -SAMUEL R- 605512516151" or "For NEFT-..."
        val forImpsPattern = Regex("""(?i)For\s+(?:IMPS|NEFT)\s*-([A-Za-z0-9 _\-&.]+?)-\s*\d""")
        return (atPattern.find(body) ?: toPattern.find(body)
            ?: infoPattern.find(body) ?: towardsPattern.find(body)
            ?: forImpsPattern.find(body))
            ?.groupValues?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }
    }
}
