package com.expenseanalyst.feature.notification.parser

/**
 * Yes Bank SMS parser.
 * Sender IDs: YESBNK, YESBK, AD-YESBNK
 *
 * Sample (debit):
 *   "INR 300.00 has been debited from your YES BANK A/c ending XX3456 for Swiggy. Avl Bal INR 6000.00"
 * Sample (credit):
 *   "INR 500.00 has been credited to your YES BANK A/c ending XX3456. Ref 778899001"
 * Sample (credit, compact):
 *   "INR 7,500.00 credited to YES BANK Ac X2919 on 01MAR25 05:01. NEFT:HDFCN52025030187729571/From:P S ANOOP-MY YES BAN. Bal INR 84,612.65."
 * Sample (debit, UPI):
 *   "YES BANK Ac X2919 debited for INR 1,000.00 on 14FEB25 11:40. UPI:504572454716/To:ps.anup.89-1@okhdfcbank. Bal INR 69,712.65."
 */
class YesBankParser : TransactionParser {

    override val bankName = "Yes Bank"

    private val senderPattern = Regex("""(?i)\byes""")
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)""")
    // Matches: "A/c ending XX3456", "Ac X2919", "a/c XX1234"
    private val accountPattern = Regex("""(?i)(?:ending|a/c(?:\s*ending)?|Ac)\s*[xX*]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref\s*(?:no\.?)?\s*:?\s*(\d+)""")
    private val forPattern = Regex("""(?i)\bfor\s+([A-Za-z0-9 _\-&.]+?)(?:\s*(?:\.|avl|available|ref|$))""")
    // UPI merchant from "/To:merchant@upi" or "/From:person name"
    private val upiToPattern = Regex("""(?i)/To:([A-Za-z0-9 _\-&.@]+?)(?:\.\s|$)""")
    private val neftFromPattern = Regex("""(?i)/From:([A-Za-z0-9 _\-&.@]+?)(?:-[A-Z]|\.\s|$)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender) && Regex("""(?i)yes\s*bank""").containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\bdebited\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\bcredited\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        val merchant = (forPattern.find(body)?.groupValues?.get(1)
            ?: upiToPattern.find(body)?.groupValues?.get(1)
            ?: neftFromPattern.find(body)?.groupValues?.get(1))
            ?.trim()?.takeIf { it.isNotBlank() && it.length < 60 }

        return ParsedTransaction(
            amount = amount,
            currencyCode = "INR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
