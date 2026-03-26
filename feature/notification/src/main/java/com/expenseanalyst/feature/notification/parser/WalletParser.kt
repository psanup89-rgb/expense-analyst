package com.expenseanalyst.feature.notification.parser

/**
 * Digital wallet parser: Apple Pay, Google Wallet, Samsung Pay.
 * These typically come as system notification titles/bodies on Android.
 *
 * Sample (Apple Pay via NFC):
 *   "Apple Pay - $12.50 at Starbucks"
 * Sample (Google Wallet):
 *   "Google Pay: $8.00 sent to John Doe"
 * Sample (Samsung Pay):
 *   "Samsung Pay: Payment of $45.00 approved at Target"
 */
class WalletParser : TransactionParser {

    override val bankName = "Digital Wallet"

    private val senderPattern = Regex("""(?i)(?:apple\s*pay|google\s*(?:pay|wallet)|samsung\s*pay)""")
    private val usdAmountPattern = Regex("""(?i)\$([\d,]+\.?\d*)""")
    private val genericAmountPattern = Regex("""(?i)(?:usd|eur|gbp|aed)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:usd|eur|gbp|aed)""")
    private val atPattern = Regex("""(?i)\bat\s+([A-Za-z0-9 _\-&.]+?)(?:\s*$|\.)""")
    private val toPattern = Regex("""(?i)\bto\s+([A-Za-z0-9 _\-&. ]+?)(?:\s*$|\.)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) || senderPattern.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\b(?:paid|payment|sent|purchase|approved|charged)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\b(?:received|refund|credited)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        // Try USD first, then generic
        val usdAmount = usdAmountPattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val genericMatch = genericAmountPattern.find(body)
        val genericAmount = (genericMatch?.groupValues?.get(1) ?: genericMatch?.groupValues?.get(2))
            ?.replace(",", "")?.toDoubleOrNull()

        val amount = usdAmount ?: genericAmount ?: return null
        val currencyCode = if (usdAmount != null) "USD"
        else {
            val text = body.uppercase()
            when {
                text.contains("EUR") -> "EUR"
                text.contains("GBP") -> "GBP"
                text.contains("AED") -> "AED"
                else -> "USD"
            }
        }

        val merchant = (atPattern.find(body) ?: toPattern.find(body))
            ?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        return ParsedTransaction(
            amount = amount,
            currencyCode = currencyCode,
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = null,
            referenceNumber = null,
            bankName = bankName
        )
    }
}
