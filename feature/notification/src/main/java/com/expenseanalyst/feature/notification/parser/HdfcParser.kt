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
    private val accountPattern = Regex("""(?i)a/c\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)(?:ref\s*(?:no|num|number)?:?\s*|upi\s*ref:?\s*)(\w+)""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) || senderPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\b(?:debited|deducted|sent)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\b(?:credited|received)\b""").containsMatchIn(body)

        if (!isDebit && !isCredit) return null

        val amountStr = amountPattern.find(body)?.groupValues?.get(1) ?: return null
        val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = extractMerchant(body)

        return ParsedTransaction(
            amount = amount,
            currencyCode = "INR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName
        )
    }

    private fun extractMerchant(body: String): String? {
        val atPattern = Regex("""(?i)\bat\s+([A-Za-z0-9 _\-&.]+?)(?:\s*(?:\.|avl|available|ref|upi|on\s+\d)|\s*$)""")
        val toPattern = Regex("""(?i)\bto\s+([A-Za-z0-9 _\-&.@]+?)(?:\s*(?:\.|avl|available|ref|upi|on\s+\d)|\s*$)""")
        return (atPattern.find(body) ?: toPattern.find(body))
            ?.groupValues?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }
    }
}
