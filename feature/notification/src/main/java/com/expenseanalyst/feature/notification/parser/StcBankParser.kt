package com.expenseanalyst.feature.notification.parser

/**
 * STC Bank (Saudi Arabia) SMS parser.
 * Sender IDs: STCPay, STCBank, STCPAY
 *
 * Sample (payment):
 *   "SAR 150.00 has been paid from your STC Pay account to Noon. Ref: TXN123456"
 * Sample (received):
 *   "SAR 200.00 received in your STC Pay account from Ahmed. Ref: TXN789012"
 * Sample (internal outward transfer):
 *   "Internal outward transfer Amount:100.00SAR To:NUMEER KOORIMMANNIL Acc:5183* At:14/04/26 15:48"
 *   "Acc:" is the RECIPIENT's account, never the user's — STC messages carry no own-account
 *   digits, so accountLast4 is always null and every STC row lands on the one STC account.
 *   Reading "Acc:" as ours created a separate "STC Bank *XXXX" account per transfer recipient.
 * Sample (card purchase — real layout, owner's report 28 Sep 2026; no currency, no "paid"):
 *   "Online Purchase\nTransaction Amount 39.32\nFrom: Hungerstation Online Payment\nCard: ******1234\nDate 28/09/2026"
 *   "From:" is the MERCHANT here (not a sender of money). The card is the user's own, but STC is one
 *   account, so it is still not used as accountLast4 — see below.
 * Sample (prepaid services):
 *   "stc prepaid services payment\nAmount: 86.25 SAR\nAt: 01/05/26 20:28"
 */
class StcBankParser : TransactionParser {

    override val bankName = "STC Bank"

    private val senderPattern = Regex("""(?i)stc(?:pay|bank)?""")
    private val amountPattern = Regex("""(?i)(?:sar|ر\.س)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:sar|ر\.س)""")
    private val refPattern = Regex("""(?i)ref:?\s*(\w+)""")
    private val toPattern = Regex("""(?i)(?:paid\s*to|to)\s+([A-Za-z0-9 _\-&.]+?)(?:\s*[.\s](?:ref|from|$))""")
    private val fromPattern = Regex("""(?i)(?:from)\s+([A-Za-z0-9 _\-&.]+?)(?:\s*[.\s](?:ref|$))""")
    // "Internal outward transfer" format: "To:RECIPIENT NAME Acc:XXXX"
    private val transferToPattern = Regex("""(?i)To:\s*([A-Za-z][A-Za-z ]+?)(?:\s+Acc:|\s*${'$'})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    // Card purchase layout: "Online Purchase … Transaction Amount 39.32 … From: MERCHANT … Card: ****1234"
    private val cardPurchasePattern = Regex("""(?i)\b(?:online|pos|international)?\s*purchase\b""")
    private val transactionAmountPattern = Regex(
        // Currency is optional and must be an upper-case code on the same line — otherwise the
        // next line's "From:" reads as currency "FRO".
        """(?i)Transaction\s+Amount[ \t]*:?[ \t]*(?:(?-i:([A-Z]{3}))[ \t]*)?([\d,]+(?:\.\d+)?)(?:[ \t]*(?-i:([A-Z]{3}))\b)?"""
    )
    private val purchaseMerchantPattern = Regex("""(?i)From\s*:\s*(.+?)\s*(?:\n|Card\s*:|Date\b|$)""")

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Card purchase: checked first — it carries none of the paid/debited/received words
        // below, which is why it used to be dropped entirely.
        if (cardPurchasePattern.containsMatchIn(body)) {
            transactionAmountPattern.find(body)?.let { m ->
                val amount = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
                val currency = m.groupValues[1].ifBlank { m.groupValues[3] }.ifBlank { "SAR" }.uppercase()
                val merchant = purchaseMerchantPattern.find(body)?.groupValues?.get(1)?.trim()
                    ?.takeIf { it.isNotBlank() && it.length < 60 }
                return ParsedTransaction(
                    amount = amount,
                    currencyCode = currency,
                    type = TransactionDirection.DEBIT,
                    merchant = merchant,
                    accountLast4 = null, // one STC account; see KDoc
                    referenceNumber = null,
                    bankName = bankName,
                    paymentMethodName = PaymentMethodDetector.detect(body) ?: "DEBIT_CARD"
                )
            }
        }

        val isTransfer = Regex("""(?i)\boutward\s+transfer\b""").containsMatchIn(body)
        val isDebit = isTransfer ||
            Regex("""(?i)\b(?:paid|debited|sent|deducted)\b""").containsMatchIn(body) ||
            Regex("""(?i)\b(?:services?\s+payment|prepaid)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\b(?:received|credited)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amountMatch = amountPattern.find(body)
        val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val ref = refPattern.find(body)?.groupValues?.get(1)

        // Prefer "To:NAME Acc:XXXX" (transfer format) over generic "to/from" patterns
        val merchant = if (isDebit) {
            transferToPattern.find(body)?.groupValues?.get(1)?.trim()
                ?: toPattern.find(body)?.groupValues?.get(1)?.trim()
        } else {
            fromPattern.find(body)?.groupValues?.get(1)?.trim()
        }
        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (isTransfer) TransactionDirection.TRANSFER
                   else if (isDebit) TransactionDirection.DEBIT
                   else TransactionDirection.CREDIT,
            merchant = merchant?.takeIf { it.isNotBlank() && it.length < 60 },
            accountLast4 = null, // see KDoc: "Acc:" is the recipient's
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body) ?: "WALLET"
        )
    }
}
