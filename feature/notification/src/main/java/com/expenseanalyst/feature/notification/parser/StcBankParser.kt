package com.expenseanalyst.feature.notification.parser

/**
 * STC Bank (Saudi Arabia) SMS parser.
 * Sender IDs: STCPay, STCBank, STCPAY
 *
 * Sample (payment):
 *   "SAR 150.00 has been paid from your STC Pay account to Noon. Ref: TXN123456"
 * Sample (received):
 *   "SAR 200.00 received in your STC Pay account from Ahmed. Ref: TXN789012"
 */
class StcBankParser : TransactionParser {

    override val bankName = "STC Bank"

    private val senderPattern = Regex("""(?i)stc(?:pay|bank)?""")
    private val amountPattern = Regex("""(?i)(?:sar|ر\.س)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:sar|ر\.س)""")
    private val refPattern = Regex("""(?i)ref:?\s*(\w+)""")
    private val toPattern = Regex("""(?i)(?:paid\s*to|to)\s+([A-Za-z0-9 _\-&.]+?)(?:\s*[.\s](?:ref|from|$))""")
    private val fromPattern = Regex("""(?i)(?:from)\s+([A-Za-z0-9 _\-&.]+?)(?:\s*[.\s](?:ref|$))""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\b(?:paid|debited|sent|deducted)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\b(?:received|credited)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amountMatch = amountPattern.find(body)
        val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = if (isDebit) toPattern.find(body)?.groupValues?.get(1)?.trim()
        else fromPattern.find(body)?.groupValues?.get(1)?.trim()

        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant?.takeIf { it.isNotBlank() && it.length < 60 },
            accountLast4 = null,
            referenceNumber = ref,
            bankName = bankName
        )
    }
}
