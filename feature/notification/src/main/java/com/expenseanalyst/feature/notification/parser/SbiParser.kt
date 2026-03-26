package com.expenseanalyst.feature.notification.parser

/**
 * State Bank of India (SBI) SMS parser.
 * Sender IDs: SBIBNK, SBIPSG, SBIINB, AD-SBIBNK
 *
 * Sample (debit):
 *   "Dear SBI Customer, Rs 500.00 debited from A/c No. XXXXXXXX1234 on 01-01-25. Info: Swiggy Food. Avl. Bal: Rs 8000.00"
 * Sample (credit):
 *   "Dear Customer, Rs 2000.00 credited to your A/c XXXXXXXX5678 on 01-01-25. Ref No 1234567890."
 */
class SbiParser : TransactionParser {

    override val bankName = "SBI"

    private val senderPattern = Regex("""(?i)\bsbi\b""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    private val accountPattern = Regex("""(?i)a/c\s*(?:no\.?)?\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref\s*(?:no\.?|num|number)?\s*:?\s*(\d+)""")
    private val infoPattern = Regex("""(?i)info:\s*([^.]+)""")

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
            ?: extractViaAt(body)

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

    private fun extractViaAt(body: String): String? {
        val pattern = Regex("""(?i)\bat\s+([A-Za-z0-9 _\-&.]+?)(?:\s*[.,]|\s*avl|\s*$)""")
        return pattern.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() && it.length < 60 }
    }
}
