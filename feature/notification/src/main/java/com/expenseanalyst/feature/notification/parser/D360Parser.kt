package com.expenseanalyst.feature.notification.parser

/**
 * D360 Bank (Saudi Arabia) SMS parser.
 * Sender IDs: D360, D360Bank
 *
 * Sample (payment):
 *   "D360 Bank: SAR 95.00 paid to Careem. Transaction ID: 9988776655. Balance: SAR 1200.00"
 * Sample (received):
 *   "D360 Bank: SAR 250.00 received. Transaction ID: 1122334455."
 * Sample (standing order transfer):
 *   "Standing Order-Debit Transfer Local\nBank:D360 Bank\nFrom:6805\nAmount:SAR 5000\nTo:ANOOP SASEEDHARAN\nTo:1616"
 */
class D360Parser : TransactionParser {

    override val bankName = "D360 Bank"

    private val senderPattern = Regex("""(?i)d360""")
    private val amountPattern = Regex("""(?i)(?:sar|ر\.س)\s*([\d,]+\.?\d*)""")
    private val txnPattern = Regex("""(?i)(?:transaction\s*id|txn\s*id|txn):?\s*(\w+)""")
    private val paidToPattern = Regex("""(?i)paid\s+to\s+([A-Za-z0-9 _\-&.]+?)(?:\s*[.\s](?:transaction|txn|balance|$))""")
    private val standingOrderPattern = Regex("""(?i)standing\s+order""")
    private val fromAccountPattern = Regex("""(?i)From\s*:\s*(\d{4})""")
    private val toNamePattern = Regex("""(?i)To\s*:\s*([A-Za-z][A-Za-z ]{2,})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) ||
        (standingOrderPattern.containsMatchIn(body) && amountPattern.containsMatchIn(body))

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Standing Order transfer: "Standing Order-Debit Transfer Local / Bank:D360 Bank / From:XXXX / Amount:SAR N / To:NAME / To:YYYY"
        if (standingOrderPattern.containsMatchIn(body)) {
            val amount = amountPattern.find(body)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull() ?: return null
            val accountLast4 = fromAccountPattern.find(body)?.groupValues?.get(1)
            val recipientName = toNamePattern.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.isNotBlank() && it.length < 80 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = TransactionDirection.TRANSFER,
                merchant = recipientName,
                accountLast4 = accountLast4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = "NET_BANKING"
            )
        }

        val isDebit = Regex("""(?i)\b(?:paid|debited|sent|deducted)\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\b(?:received|credited)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val ref = txnPattern.find(body)?.groupValues?.get(1)
        val merchant = paidToPattern.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() && it.length < 60 }

        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = null,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
