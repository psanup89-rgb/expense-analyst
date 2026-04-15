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
    private val transferAccPattern = Regex("""(?i)Acc:\s*(\d{3,4})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isTransfer = Regex("""(?i)\boutward\s+transfer\b""").containsMatchIn(body)
        val isDebit = isTransfer || Regex("""(?i)\b(?:paid|debited|sent|deducted)\b""").containsMatchIn(body)
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
        val accountLast4 = transferAccPattern.find(body)?.groupValues?.get(1)

        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (isTransfer) TransactionDirection.TRANSFER
                   else if (isDebit) TransactionDirection.DEBIT
                   else TransactionDirection.CREDIT,
            merchant = merchant?.takeIf { it.isNotBlank() && it.length < 60 },
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body) ?: "WALLET"
        )
    }
}
