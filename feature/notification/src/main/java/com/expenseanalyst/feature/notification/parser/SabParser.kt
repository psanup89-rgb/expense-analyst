package com.expenseanalyst.feature.notification.parser

/**
 * SAB (Saudi Awwal Bank) card messages. Real layout (owner's inbox, Aug 2026; numbers changed):
 *   "Online Purchase\nBy: ***1234;mada\nFrom: ***5678\nAmount: SAR 64.17\nAt: HUNGERSTATION LLC××\nDate: 2026-08-09 20:00:00"
 * "By:" is the user's card, "From:" the user's account (both masked with stars — Al Rajhi writes
 * "By:1234" with no stars, Emirates NBD "By: XX1234"). There was no SAB parser: these were
 * picked up first by Al Rajhi's old catch-all fingerprint (merchant lost), then by Emirates NBD's
 * over-broad body fingerprint (filed under the wrong bank). Refund wording flips the direction.
 *
 * Sentence layout (seen from Nov 2026; numbers changed):
 *   "PoS Purchase\nSAB mada(Apple Pay) Card (1234) from (***567) was used at Soul Moment Est for SAR 18.00 on 2026-11-23 16:33"
 * The card is in brackets, the merchant follows "was used at", the amount "for SAR". No parser
 * read it — it fell to GenericParser as "Unknown Bank" with no card, so every such purchase landed
 * on the Unknown account and correcting the account by hand could not carry over to the next one.
 */
class SabParser : TransactionParser {

    override val bankName = "SAB"

    private val senderPattern = Regex("""(?i)^\s*(?:SAB|SABB)\b""")
    // SAB-specific shape: a starred card number on "By:"
    private val bodyFingerprint = Regex("""(?i)\bBy:\s*\*{2,}\d{4}\s*;""")
    private val amountPattern = Regex("""(?i)Amount\s*:\s*(?:SAR|SR)\s*([\d,]+(?:\.\d+)?)""")
    private val atPattern = Regex("""(?i)\bAt\s*:\s*([^\n]+)""")
    private val cardPattern = Regex("""(?i)\bBy:\s*\**(\d{4})""")
    private val refundPattern = Regex("""(?i)\brefund|reversal\b""")

    // Sentence layout: "SAB mada(Apple Pay) Card (1234) from (***567) was used at X for SAR 18.00 on …"
    private val sentenceFingerprint = Regex("""(?i)\bSAB\b.{0,60}\bCard\s*\(\d{4}\).{0,60}\bwas\s+used\s+at\b""", RegexOption.DOT_MATCHES_ALL)
    private val sentenceCardPattern = Regex("""(?i)\bCard\s*\((\d{4})\)""")
    private val sentenceMerchantPattern = Regex("""(?i)\bwas\s+used\s+at\s+(.+?)\s+for\s+(?:SAR|SR)\b""")
    private val sentenceAmountPattern = Regex("""(?i)\bfor\s+(?:SAR|SR)\s*([\d,]+(?:\.\d+)?)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) || bodyFingerprint.containsMatchIn(body) ||
            sentenceFingerprint.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        if (sentenceFingerprint.containsMatchIn(body)) {
            val amount = sentenceAmountPattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
                ?: return null
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = if (refundPattern.containsMatchIn(body)) TransactionDirection.CREDIT else TransactionDirection.DEBIT,
                merchant = sentenceMerchantPattern.find(body)?.groupValues?.get(1)?.trim()
                    ?.takeIf { it.isNotBlank() && it.length < 60 },
                accountLast4 = sentenceCardPattern.find(body)?.groupValues?.get(1),
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = PaymentMethodDetector.detect(body)
                    ?: if (body.contains("mada", ignoreCase = true)) "DEBIT_CARD" else null
            )
        }
        val amount = amountPattern.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (refundPattern.containsMatchIn(body)) TransactionDirection.CREDIT else TransactionDirection.DEBIT,
            merchant = atPattern.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() && it.length < 60 },
            accountLast4 = cardPattern.find(body)?.groupValues?.get(1),
            referenceNumber = null,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
                ?: if (body.contains("mada", ignoreCase = true)) "DEBIT_CARD" else null
        )
    }
}
