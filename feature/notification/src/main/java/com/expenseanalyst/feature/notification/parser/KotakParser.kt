package com.expenseanalyst.feature.notification.parser

/**
 * Kotak Mahindra Bank SMS parser.
 * Sender IDs: KOTAKB, KOTAK, AD-KOTAK
 *
 * Sample (debit):
 *   "INR 750.00 debited from Kotak Bank A/c XX5678 on 01-Jan-25 via UPI to Uber. Ref 111222333"
 * Sample (credit):
 *   "INR 3000.00 credited to your Kotak A/c XX5678 on 01-Jan-25. Ref 444555666"
 */
class KotakParser : TransactionParser {

    override val bankName = "Kotak Bank"

    private val senderPattern = Regex("""(?i)kotak""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    private val accountPattern = Regex("""(?i)a/c\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref\s*(?:no\.?)?\s*:?\s*(\d+)""")
    private val merchantPattern = Regex("""(?i)(?:to|via upi to|at)\s+([A-Za-z0-9 _\-&.]+?)(?:\s*[.\s](?:ref|on\s+\d|avl|$))""")

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
        val merchant = merchantPattern.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        return ParsedTransaction(
            amount = amount,
            currencyCode = "INR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
