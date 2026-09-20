package com.expenseanalyst.feature.notification.parser

/**
 * Al Rajhi Bank (Saudi Arabia) SMS parser.
 * Sender IDs: AlRajhi, ALRAJHI, 74100
 *
 * Sample (purchase/debit, classic):
 *   "Purchase of SAR 250.00 was made using your card ending 1234 at Jarir Bookstore on 01/01/2025"
 * Sample (purchase/debit, colon format):
 *   "Online Purchase By:7573 ;Visa Amount:228.24 SAR At:Amazon SA Balance:47242.85 SAR 24/3/26 13:20"
 *   "Online Purchase Card:7573 ;Visa Amount:23USD(86.36 SAR) ... At: CLAUDE.AI Balance:47892.18 SAR"
 * Sample (credit):
 *   "SAR 1500.00 has been credited to your account ending 5678. Ref: 987654321"
 * Sample (Arabic):
 *   "تم خصم مبلغ 350.00 ر.س من حسابك"
 * Sample (MOI government payment):
 *   "MOI Payments From:6805 Amount:SR 400 Provider:Residents Services Service:Extend Exit Re-entry Visa Duration 26/4/14 18:57"
 * Sample (standing order / external transfer — note From: is the digits here, opposite of
 * the "Credit Transfer Internal" sample below where From: is a name and To: is the digits):
 *   "Standing Order-Debit Transfer Local\nFrom:6805\nAmount:SAR 5000\nTo:ANOOP SASEEDHARAN\nTo:1616"
 * Sample (bill payment — same Biller:/Service: field vocabulary as the Mubasher app, but this
 * one arrives from Al Rajhi's own sender):
 *   "Bill Payment\nFrom:6805\nAmount:SAR 240\nBiller:125\nService:ENBD PAYMENTS\nBill:01600000025919"
 */
class AlRajhiParser : TransactionParser {

    override val bankName = "Al Rajhi Bank"

    private val senderPattern = Regex("""(?i)(?:alrajhi|al.rajhi|rajhi|74100)""")
    private val transferFingerprintPattern = Regex("""(?i)(?:Credit|Debit)\s+Transfer\s+Internal""")
    // MOI (Ministry of Interior) government service payments via Al Rajhi
    private val moiFingerprintPattern = Regex("""(?i)MOI\s+Payments""")
    // Standing order / external transfer — without these, this shape falls through every
    // branch below and gets claimed by D360Parser's own standing-order fingerprint instead,
    // mislabeling the bank as "D360 Bank".
    private val standingOrderPattern = Regex("""(?i)standing\s+order""")
    private val standingOrderToNamePattern = Regex("""(?i)To\s*:\s*([A-Za-z][A-Za-z ]{2,})""")
    // Bill payment — without these, this shape falls through and gets claimed by
    // MubasherParser's own Biller:/Service: fingerprint instead, mislabeling the bank as
    // "Mubasher" (the app Al Rajhi's bill-pay flow happens to share field vocabulary with).
    private val billPaymentPattern = Regex("""(?i)bill\s*payment""")
    private val billServicePattern = Regex("""(?i)Service\s*:\s*(.+?)(?:\n|$)""")
    // Shared by both branches above: "From:6805" — the user's own account being debited.
    private val fromAccountDigitsPattern = Regex("""(?i)From\s*:\s*(\d{4})""")
    private val moiAccountPattern = Regex("""(?i)From:\s*(\d{3,4})""")
    private val moiProviderPattern = Regex("""(?i)Provider:\s*([A-Za-z][A-Za-z ]+?)(?:\s+Service:|\s*${'$'})""")
    private val moiServicePattern = Regex("""(?i)Service:\s*([A-Za-z][A-Za-z 0-9\-]+?)(?:\s+Duration|\s+\d|\s*${'$'})""")
    private val amountSarPattern = Regex("""(?i)(?:sar|ر\.س|sr)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:sar|ر\.س|sr)""")
    // Handles: "card ending 1234", "Card:7573", "By:7573", "account no. 5678"
    private val cardPattern = Regex("""(?i)(?:card|account|by)[:\s]*(?:ending|no\.?|number)?\s*[xX*]*(\d{4})""")
    // Matches payment method label after card digits, e.g. ";Visa-Apple Pay", ";Visa", ";Mastercard"
    private val paymentMethodPattern = Regex("""(?i);\s*(?:Visa|Mastercard|Mada|Amex|Discover)[-\s]*([A-Za-z ]+)?""")
    private val refPattern = Regex("""(?i)ref(?:erence)?:?\s*(\w+)""")
    // Transfer-specific patterns
    private val transferToPattern = Regex("""(?i)To\s*:\s*(\d{4})""")
    // Matches "From:MOHAMATHU PILLAI" (name form) but not "From:5119" (digit form)
    private val transferFromNamePattern = Regex("""(?i)From\s*:\s*([A-Za-z][A-Za-z ]{2,})""")
    // Stops at newline OR known Al Rajhi field names (Amount, Fee, Balance, etc.) or end of string.
    // Includes * for merchants like "GOOGLE*PA", "OPENAI *C"
    private val atPattern = Regex(
        """(?i)\bat[:\s]\s*([A-Za-z0-9][A-Za-z0-9 _\-&./*]*?)(?=\s*\n|\s+(?:Amount|Fee|Balance|Ref|Exchange|Country|Total|Available|on\s+\d)|\s*${'$'})"""
    )

    // Deliberately NOT a generic "purchase...SAR...balance/amount" body fingerprint —
    // that used to sit here and matched practically any Saudi bank's card-purchase SMS
    // shape, so Al Rajhi (registered first in ParserRegistry) was silently stealing real
    // Emirates NBD and D360 Bank messages whenever their sender didn't literally contain
    // "alrajhi". Every body-only fingerprint below is Al-Rajhi-specific wording (MOI
    // Payments, Standing Order, Bill Payment+Biller/Service, Credit/Debit Transfer
    // Internal) precisely so this parser only self-claims a message when the sender
    // doesn't match AND the body itself is unambiguously an Al Rajhi shape.
    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) ||
        transferFingerprintPattern.containsMatchIn(body) ||
        moiFingerprintPattern.containsMatchIn(body) ||
        standingOrderPattern.containsMatchIn(body) ||
        (billPaymentPattern.containsMatchIn(body) && billServicePattern.containsMatchIn(body))

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Handle MOI government payment: "MOI Payments From:6805 Amount:SR 400 Provider:X Service:Y"
        if (moiFingerprintPattern.containsMatchIn(body)) {
            val amountMatch = amountSarPattern.find(body)
            val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
                ?.replace(",", "")?.toDoubleOrNull() ?: return null
            val accountLast4 = moiAccountPattern.find(body)?.groupValues?.get(1)
            val provider = moiProviderPattern.find(body)?.groupValues?.get(1)?.trim()
            val service = moiServicePattern.find(body)?.groupValues?.get(1)?.trim()
            val merchant = when {
                provider != null && service != null -> "$provider - $service"
                service != null -> service
                provider != null -> provider
                else -> "MOI Payment"
            }.takeIf { it.length < 80 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = TransactionDirection.DEBIT,
                merchant = merchant,
                accountLast4 = accountLast4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = "NET_BANKING"
            )
        }

        // Standing order / external transfer: "Standing Order-Debit Transfer Local /
        // From:XXXX / Amount:SAR N / To:NAME / To:YYYY". Field convention is the mirror image
        // of "Credit Transfer Internal" below — here From: is the user's own account digits
        // and To: is the recipient's name.
        if (standingOrderPattern.containsMatchIn(body)) {
            val amountMatch = amountSarPattern.find(body)
            val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
                ?.replace(",", "")?.toDoubleOrNull() ?: return null
            val accountLast4 = fromAccountDigitsPattern.find(body)?.groupValues?.get(1)
            val merchant = standingOrderToNamePattern.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotBlank() && it.length < 80 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = TransactionDirection.TRANSFER,
                merchant = merchant,
                accountLast4 = accountLast4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = "NET_BANKING"
            )
        }

        // Bill payment: "Bill Payment / From:XXXX / Amount:SAR N / Biller:.. / Service:.. / Bill:.."
        if (billPaymentPattern.containsMatchIn(body) && billServicePattern.containsMatchIn(body)) {
            val amountMatch = amountSarPattern.find(body)
            val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
                ?.replace(",", "")?.toDoubleOrNull() ?: return null
            val accountLast4 = fromAccountDigitsPattern.find(body)?.groupValues?.get(1)
            val merchant = billServicePattern.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotBlank() && it.length < 80 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = TransactionDirection.PAYMENT,
                merchant = merchant,
                accountLast4 = accountLast4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = PaymentMethodDetector.detect(body)
            )
        }

        // Handle internal transfer format: "Credit Transfer Internal / Amount:SAR N / To:XXXX / From:NAME / From:YYYY"
        if (transferFingerprintPattern.containsMatchIn(body)) {
            val amountMatch = amountSarPattern.find(body)
            val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
                ?.replace(",", "")?.toDoubleOrNull() ?: return null
            val accountLast4 = transferToPattern.find(body)?.groupValues?.get(1)
            val merchant = transferFromNamePattern.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotBlank() && it.length < 80 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = TransactionDirection.TRANSFER,
                merchant = merchant,
                accountLast4 = accountLast4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = "NET_BANKING"
            )
        }

        val isDebit = Regex("""(?i)\b(?:pos\s+purchase|purchase|purchased|debited|deducted|خصم|مشتريات)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\b(?:credited|received|أضيف|إيداع)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amountMatch = amountSarPattern.find(body)
        val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountLast4 = cardPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = atPattern.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        val detectedPaymentMethod = PaymentMethodDetector.detect(body)

        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = detectedPaymentMethod
        )
    }
}
