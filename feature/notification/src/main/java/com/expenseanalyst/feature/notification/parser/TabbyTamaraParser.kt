package com.expenseanalyst.feature.notification.parser

/**
 * Parses the purchase-time SPLIT CONFIRMATION SMS from Tabby or Tamara (BNPL providers) —
 * distinct from [TamaraStatementParser], which only handles the later payment-DUE reminder and
 * routes to the Bills table, never touching expense totals.
 *
 * A match here is flagged [ParsedTransaction.isBnplConfirmation] and routed by
 * `PendingNotificationManager` to reclassify the merchant's own already-captured full-amount
 * expense (if one exists, matched by amount + merchant + same calendar day) as a `PAYMENT`
 * under the "Split Payments" category — structurally excluded from every spend total, per the
 * owner's explicit decision that these purchases should never count toward "spent this month."
 *
 * ⚠️ UNVERIFIED. No real Tabby/Tamara purchase-confirmation SMS sample was available when this
 * was written. Built by extrapolating from [TamaraStatementParser]'s one confirmed real sample
 * (a payment-*due* reminder, not a purchase confirmation — a different message shape entirely)
 * plus Tabby/Tamara's typical public wording. Check this against a real message before trusting
 * it in production, and expect the regexes below to need adjustment.
 *
 * Sample (unverified, best guess):
 *   "Your purchase of 400.00 SAR at Noon has been split into 4 payments of 100.00 SAR.
 *    First payment charged today."
 *   "You've split your 400 SAR purchase into 4 interest-free instalments of 100 SAR with Tabby."
 */
class TabbyTamaraParser : TransactionParser {

    override val bankName = "Tabby / Tamara"

    private val senderPattern = Regex("""(?i)\b(tabby|tamara)\b""")

    // Distinguishes a purchase-time split confirmation from a due-date reminder
    // (TamaraStatementParser's "payment of X ... due in N days" shape).
    private val splitFingerprint = Regex(
        """(?i)split\s+(?:your\s+)?(?:purchase\s+)?into\s+\d+\s+(?:payments|instal?ments)""" +
            """|\d+\s+interest-free\s+(?:payments|instal?ments)"""
    )

    // "split into 4 payments of 100.00 SAR" / "4 interest-free instalments of 100 SAR"
    private val installmentPattern = Regex(
        """(?i)(\d+)\s+(?:interest-free\s+)?(?:payments|instal?ments)\s+of\s+([\d,]+\.?\d*)\s*([A-Z]{3})?"""
    )

    // "purchase of 400.00 SAR" — the stated total, when the SMS states it directly
    private val totalPattern = Regex(
        """(?i)purchase\s+of\s+([\d,]+\.?\d*)\s*([A-Z]{3})?"""
    )

    // "at Noon" — same idiom as AlRajhiParser's atPattern. Stops at a following clause/sentence.
    private val atMerchantPattern = Regex(
        """(?i)\bat\s+([A-Za-z0-9][A-Za-z0-9 &'.-]{1,40}?)(?=\s+has\s+been|\.|\s*$)"""
    )

    /**
     * The wording Tabby ACTUALLY sends (first real sample, 27 Sep 2026), which the split
     * fingerprint above never matched — so GenericParser caught it instead and recorded the full
     * price as an ordinary expense, double-counting it against the instalments on the card:
     *   "Your SAR 599.00 purchase at CENTREPOINT is confirmed. Track your upcoming payments
     *    with the Tabby app: <link>"
     * No instalment count is stated; BnplInstallmentMatcher infers it from the card charges.
     */
    private val confirmedPurchasePattern = Regex(
        """(?i)your\s+([A-Z]{3})\s+([\d,]+(?:\.\d+)?)\s+purchase\s+at\s+(.+?)\s+is\s+confirmed"""
    )

    /**
     * The wording Tamara ACTUALLY sends (real sample from the owner, 27 Sep 2026):
     *   "Split in 3 payment
     *    confirmation:
     *    Store: Ikea Store
     *    Order: 1,585.75 SAR
     *    Date: 22/09/2026
     *    Visit the app for more details."
     * The body never says "Tamara", so this fingerprint alone identifies the provider; unlike
     * Tabby it states the instalment count, which [instalmentCountOf] hands to the matcher.
     * Until this existed no parser matched it, and unmatched messages are not saved — every
     * Tamara purchase was silently dropped, leaving only the "At: Tamara" card charges.
     */
    private val tamaraSplitPattern = Regex("""(?i)split\s+in\s+(\d+)\s+payments?\s*confirmation""")
    private val storePattern = Regex("""(?i)Store\s*:\s*(.+)""")
    private val orderAmountPattern = Regex(
        """(?i)Order\s*:\s*(?:([A-Z]{3})\s*)?([\d,]+(?:\.\d+)?)\s*([A-Z]{3})?"""
    )

    /** The instalment count a confirmation states ("Split in 3 payment confirmation"), if any. */
    fun instalmentCountOf(body: String): Int? =
        tamaraSplitPattern.find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: installmentPattern.find(body)?.groupValues?.get(1)?.toIntOrNull()

    // "for your Samsung order" — same shape as TamaraStatementParser's orderPattern.
    private val orderMerchantPattern = Regex("""(?i)for\s+(?:your\s+)?(.+?)\s+order""")

    override fun canParse(sender: String, body: String): Boolean =
        tamaraSplitPattern.containsMatchIn(body) ||
            (senderPattern.containsMatchIn(sender) || senderPattern.containsMatchIn(body)) &&
            (splitFingerprint.containsMatchIn(body) || confirmedPurchasePattern.containsMatchIn(body))

    override fun parse(sender: String, body: String): ParsedTransaction? {
        if (tamaraSplitPattern.containsMatchIn(body)) {
            val m = orderAmountPattern.find(body) ?: return null
            val amount = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            val currency = m.groupValues[1].ifBlank { m.groupValues[3] }.ifBlank { "SAR" }.uppercase()
            val shop = storePattern.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotBlank() && it.length < 60 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = currency,
                type = TransactionDirection.PAYMENT,
                merchant = shop,
                accountLast4 = null,
                referenceNumber = null,
                bankName = "Tamara",
                paymentMethodName = "CREDIT_CARD",
                isBnplConfirmation = true
            )
        }

        confirmedPurchasePattern.find(body)?.let { m ->
            val amount = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            val shop = m.groupValues[3].trim().takeIf { it.isNotBlank() && it.length < 60 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = m.groupValues[1].uppercase(),
                type = TransactionDirection.PAYMENT,
                merchant = shop,
                accountLast4 = null,
                referenceNumber = null,
                bankName = providerOf(sender, body),
                paymentMethodName = "CREDIT_CARD",
                isBnplConfirmation = true
            )
        }

        val installmentMatch = installmentPattern.find(body) ?: return null
        val installmentCount = installmentMatch.groupValues[1].toIntOrNull()
            ?.takeIf { it > 0 } ?: return null
        val installmentAmount = installmentMatch.groupValues[2].replace(",", "").toDoubleOrNull()
            ?: return null
        val installmentCurrency = installmentMatch.groupValues[3].takeIf { it.isNotBlank() }

        val totalMatch = totalPattern.find(body)
        val totalAmount = totalMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: (installmentAmount * installmentCount)
        val currency = totalMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() }
            ?: installmentCurrency
            ?: "SAR"

        val merchant = (
            atMerchantPattern.find(body)?.groupValues?.get(1)
                ?: orderMerchantPattern.find(body)?.groupValues?.get(1)
            )
            ?.trim()
            ?.replaceFirstChar { it.uppercase() }
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        val provider = providerOf(sender, body)

        return ParsedTransaction(
            amount = totalAmount,
            currencyCode = currency,
            type = TransactionDirection.PAYMENT,
            merchant = merchant,
            accountLast4 = null,
            referenceNumber = null,
            bankName = provider,
            paymentMethodName = PaymentMethodDetector.detect(body) ?: "CREDIT_CARD",
            isBnplConfirmation = true
        )
    }

    private fun providerOf(sender: String, body: String): String =
        if (Regex("""(?i)tabby""").containsMatchIn(sender + body)) "Tabby" else "Tamara"
}
