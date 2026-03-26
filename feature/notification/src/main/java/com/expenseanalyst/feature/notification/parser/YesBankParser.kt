package com.expenseanalyst.feature.notification.parser

/**
 * Yes Bank SMS parser.
 * Sender IDs: YESBNK, YESBK, AD-YESBNK
 *
 * Sample (debit):
 *   "INR 300.00 has been debited from your YES BANK A/c ending XX3456 for Swiggy. Avl Bal INR 6000.00"
 * Sample (credit):
 *   "INR 500.00 has been credited to your YES BANK A/c ending XX3456. Ref 778899001"
 */
class YesBankParser : TransactionParser {

    override val bankName = "Yes Bank"

    private val senderPattern = Regex("""(?i)\byes\b""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    private val accountPattern = Regex("""(?i)(?:ending|a/c(?:\s*ending)?)\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref\s*(?:no\.?)?\s*:?\s*(\d+)""")
    private val forPattern = Regex("""(?i)\bfor\s+([A-Za-z0-9 _\-&.]+?)(?:\s*(?:\.|avl|available|ref|$))""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) && Regex("""(?i)yes\s*bank""").containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\bdebited\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\bcredited\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = forPattern.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

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
}
