package com.expenseanalyst.feature.notification.parser

/**
 * ICICI Bank SMS parser.
 * Sender IDs: ICICIB, ICICIBANK, AD-ICICIB
 *
 * Sample (debit):
 *   "ICICI Bank Acct XX1234: Rs 450.00 debited on 01-Jan-25. Info: UPI/123456/Zomato. Avl Bal: Rs 5500.00"
 * Sample (credit):
 *   "ICICI Bank Acct XX1234: Rs 1500.00 credited on 01-Jan-25. Ref 98765432."
 */
class IciciParser : TransactionParser {

    override val bankName = "ICICI Bank"

    private val senderPattern = Regex("""(?i)icici""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    private val accountPattern = Regex("""(?i)(?:acct|a/c|account)\s*(?:no\.?)?\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref\s*(?:no\.?)?\s*:?\s*(\d+)""")
    private val infoPattern = Regex("""(?i)info:\s*(?:upi/\d+/)?([^.]+)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\bdebited\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\bcredited\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = infoPattern.find(body)?.groupValues?.get(1)?.trim()
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
