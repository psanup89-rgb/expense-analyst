package com.expenseanalyst.feature.notification.parser

/**
 * Emirates NBD (UAE) SMS parser.
 * Sender ID: EmiratesNBD
 *
 * Sample (POS):
 *   "POS Purchase (Apple Pay)\nCard: Visa card XX4388\nAmount: SAR 36.00\nMerchant: STARBUCKS-S876\nIn: SAUDI ARABIA\nRemaining limit SAR 18,117.95\nOn: 2026-03-28 15:54:43"
 * Sample (Online):
 *   "Online Purchase (Apple Pay)\nCard: Credit card XX4388\nMerchant: Temu.com\nAmount: SAR 14.36\nOn: 2026-03-24 02:14:13\nRemaining limit SAR 18,172.95"
 * Sample (Credit card payment credited):
 *   "Credit Card: Credited\nCard : XX4388;Credit Card Visa\nAmount: SAR 39.00\nBalance: SAR 18,156.95\nDate: 29-03-2026"
 * Sample (POS Reversal):
 *   "POS Reversal\nTo: XX9731; Visa Credit\nAmount: NZD 3,842.51\nFrom: Air New Zealand\nIn NEW ZEALAND\nRemaining limit SAR: 28,800.00\nOn: 2026-04-17 15:26:49"
 */
class EmiratesNbdParser : TransactionParser {

    override val bankName = "Emirates NBD"

    private val senderPattern = Regex("""(?i)(?:emirates\s*nbd|enbd|emiratesnbd)""")
    private val bodyFingerprintPattern = Regex(
        """(?i)(?:(?:POS|Online)\s+Purchase|POS\s+Reversal|Credit\s+Card:\s*Credited).*(?:Card\s*:|Merchant:|Amount:|To:)""",
        RegexOption.DOT_MATCHES_ALL
    )

    private val purchasePattern = Regex("""(?i)(POS|Online)\s+Purchase""")

    // "POS Reversal" — money returned to card
    private val reversalPattern = Regex("""(?i)POS\s+Reversal""")
    // "To: XX9731; Visa Credit"
    private val reversalCardPattern = Regex("""(?i)To:\s*XX(\d{4})""")
    // "From: Air New Zealand"
    private val reversalMerchantPattern = Regex("""(?i)From:\s*(.+)""")

    // "Credit Card: Credited" — payment received to credit card
    private val creditedPattern = Regex("""(?i)Credit\s+Card:\s*Credited""")
    // "Card : XX4388;Credit Card Visa" — different format from purchase messages
    private val creditedCardPattern = Regex("""(?i)Card\s*:\s*XX(\d{4})""")
    private val paymentMethodParenPattern = Regex("""\(([^)]+)\)""")
    private val cardPattern = Regex("""(?i)Card:\s*(?:Visa|Credit|Debit|Mastercard|Mada|Amex)?\s*card\s*XX(\d{4})""")
    private val amountPattern = Regex("""(?i)Amount:\s*([A-Z]{3})\s*([\d,]+\.?\d*)""")
    private val merchantPattern = Regex("""(?i)Merchant:\s*(.+)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) || bodyFingerprintPattern.containsMatchIn(body)

    private fun inferCardType(body: String): String? {
        // "Card: Visa card XX4388" → Visa = could be credit or debit, but Emirates NBD Visa is typically credit
        // "Card: Credit card XX4388" → explicit credit
        // "Card: Debit card XX4388" → explicit debit
        return when {
            Regex("""(?i)Card:\s*(?:Credit|Visa|Mastercard|Amex)\s+card""").containsMatchIn(body) -> "CREDIT_CARD"
            Regex("""(?i)Card:\s*Debit\s+card""").containsMatchIn(body) -> "DEBIT_CARD"
            Regex("""(?i)Card:\s*Mada\s+card""").containsMatchIn(body) -> "DEBIT_CARD"
            else -> null
        }
    }

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Handle "Credit Card: Credited" — payment applied to credit card
        if (creditedPattern.containsMatchIn(body)) {
            val amountMatch = amountPattern.find(body) ?: return null
            val currencyCode = amountMatch.groupValues[1]
            val amount = amountMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            val accountLast4 = creditedCardPattern.find(body)?.groupValues?.get(1)
            return ParsedTransaction(
                amount = amount,
                currencyCode = currencyCode,
                type = TransactionDirection.PAYMENT,
                merchant = null,
                accountLast4 = accountLast4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = "CREDIT_CARD"
            )
        }

        // Handle "POS Reversal" — refund/chargeback credited back to card
        if (reversalPattern.containsMatchIn(body)) {
            val amountMatch = amountPattern.find(body) ?: return null
            val currencyCode = amountMatch.groupValues[1]
            val amount = amountMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            val accountLast4 = reversalCardPattern.find(body)?.groupValues?.get(1)
            val merchant = reversalMerchantPattern.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotBlank() && it.length < 60 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = currencyCode,
                type = TransactionDirection.CREDIT,
                merchant = merchant,
                accountLast4 = accountLast4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = inferCardType(body) ?: "CREDIT_CARD"
            )
        }

        if (!purchasePattern.containsMatchIn(body)) return null

        val amountMatch = amountPattern.find(body) ?: return null
        val currencyCode = amountMatch.groupValues[1]
        val amount = amountMatch.groupValues[2]
            .replace(",", "").toDoubleOrNull() ?: return null

        val merchant = merchantPattern.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        val accountLast4 = cardPattern.find(body)?.groupValues?.get(1)

        // Detect payment method: wallet overlay first, then infer from card type in body
        val detectedPaymentMethod = PaymentMethodDetector.detect(body)
            ?: inferCardType(body)

        return ParsedTransaction(
            amount = amount,
            currencyCode = currencyCode,
            type = TransactionDirection.DEBIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = null,
            bankName = bankName,
            paymentMethodName = detectedPaymentMethod
        )
    }
}
