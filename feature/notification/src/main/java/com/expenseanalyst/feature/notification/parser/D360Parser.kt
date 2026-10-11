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
 *   "Standing Order-Debit Transfer Local\nBank:D360 Bank\nFrom:1234\nAmount:SAR 5000\nTo:RECIPIENT NAME\nTo:5678"
 * Sample (international transfer, real wording Oct 2026):
 *   "International transfer\nAmount: INR 2,556.77 (SAR 100.00)\nTo: RECIPIENT NAME\n<account no>\nCountry:India\nThrough:THUNES\nOn: …"
 * Sample (incoming transfer, real wording Oct 2026):
 *   "Incoming Transfer: AlRajhi Bank\nAmount: SAR 6,000.00\nFrom: SENDER NA****\nIBAN: ****1234\nat: …"
 * Neither says paid/sent/received, so until Oct 2026 both were dropped. Every account number in
 * them is the counterparty's, never the user's, so neither sets accountLast4.
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
    private val internationalPattern = Regex("""(?i)^\s*international\s+transfer\b""")
    // The SAR amount in brackets is what left the account: "Amount: INR 2,556.77 (SAR 100.00)"
    private val bracketSarPattern = Regex("""(?i)\(\s*SAR\s*([\d,]+\.?\d*)\s*\)""")
    private val incomingPattern = Regex("""(?i)^\s*incoming\s+transfer\b""")
    // The sender's name arrives masked: "From: SENDER NA****"
    private val fromNamePattern = Regex("""(?i)From\s*:\s*([A-Za-z][A-Za-z .'-]*[A-Za-z])\**""")

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

        // International transfer out. The SAR equivalent is what left the account; the
        // destination amount is in the recipient's currency.
        if (internationalPattern.containsMatchIn(body)) {
            val amount = (bracketSarPattern.find(body) ?: amountPattern.find(body))?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = TransactionDirection.TRANSFER,
                merchant = toNamePattern.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.length < 80 },
                accountLast4 = null,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = "NET_BANKING"
            )
        }

        // Incoming transfer from another bank. A TRANSFER, not income: it is often the user's own
        // money from their other bank, and the masked sender name can't be matched to their
        // own-account rule — as INCOME it would inflate Received. Unclassified, it counts toward
        // nothing until the user classifies it in Review, which is then remembered for the name.
        if (incomingPattern.containsMatchIn(body)) {
            val amount = amountPattern.find(body)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                currencyCode = "SAR",
                type = TransactionDirection.TRANSFER,
                merchant = fromNamePattern.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.length < 80 },
                accountLast4 = null,
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
