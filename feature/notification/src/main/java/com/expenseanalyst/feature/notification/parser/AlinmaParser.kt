package com.expenseanalyst.feature.notification.parser

/**
 * Alinma Bank (Saudi Arabia) SMS parser.
 * Sender IDs: Alinma, ALINMA
 *
 * Sample (POS debit):
 *   "Your Alinma card ending 4321 has been used for SAR 180.00 at Extra Stores on 01/01/2025. Available balance: SAR 4200.00"
 * Sample (credit):
 *   "SAR 5000.00 has been credited to your Alinma account ending 4321. Ref: 112233445"
 */
class AlinmaParser : TransactionParser {

    override val bankName = "Alinma Bank"

    private val senderPattern = Regex("""(?i)alinma""")
    private val amountPattern = Regex("""(?i)(?:sar|ر\.س)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:sar|ر\.س)""")
    private val cardPattern = Regex("""(?i)(?:card|account)\s*(?:ending)?\s*(\d{4})""")
    private val refPattern = Regex("""(?i)ref:?\s*(\w+)""")
    private val atPattern = Regex("""(?i)\bat[:\s]\s*([A-Za-z0-9][A-Za-z0-9 _\-&./]*?)(?=\s+on\s+\d|\.\s*(?:ref\b|${'$'})|\s+(?:balance|available|ref)\b|\s+[A-Z][a-z]+:|\s*${'$'})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\b(?:used|debited|purchase|deducted)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\bcredited\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        // Find the first SAR amount (skip available balance)
        val amountMatch = amountPattern.find(body)
        val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountLast4 = cardPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = atPattern.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
