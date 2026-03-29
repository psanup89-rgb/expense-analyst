package com.expenseanalyst.feature.notification.parser

/**
 * Parser for Mubasher App bill payment notifications.
 *
 * Mubasher is a Saudi financial platform used to pay bills (credit cards, utilities, etc.).
 * Sender ID varies — detection is based on body content fingerprint.
 *
 * Sample:
 *   "Reason:Bills Payment - Mubasher App
 *    Bill Payment
 *    From:6805
 *    Amount:SAR 240
 *    Biller:125
 *    Service:ENBD PAYMENTS
 *    Bill:01600000025919
 *    26/3/28 22:10"
 */
class MubasherParser : TransactionParser {

    override val bankName = "Mubasher"

    private val senderPattern = Regex("""(?i)mub(?:asher|shr)?""")
    private val bodyFingerprintPattern = Regex(
        """(?i)(?:Reason\s*:.*(?:Bills?\s*Payment|Bill\s*Transfer|Payment\s*Transfer)|(?:Biller|Service)\s*:)""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val amountPattern = Regex("""(?i)Amount\s*:\s*SAR\s*([\d,]+\.?\d*)""")
    private val fromPattern = Regex("""(?i)From\s*:\s*(\d{4})""")
    private val servicePattern = Regex("""(?i)Service\s*:\s*(.+?)(?:\n|$)""")
    private val billRefPattern = Regex("""(?i)\bBill\s*:\s*(\S+)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) || bodyFingerprintPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val amountMatch = amountPattern.find(body) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        val accountLast4 = fromPattern.find(body)?.groupValues?.get(1)
        val merchant = servicePattern.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 80 }
        val reference = billRefPattern.find(body)?.groupValues?.get(1)?.trim()

        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = TransactionDirection.PAYMENT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = reference,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
