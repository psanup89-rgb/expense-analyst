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
 */
class AlRajhiParser : TransactionParser {

    override val bankName = "Al Rajhi Bank"

    private val senderPattern = Regex("""(?i)(?:alrajhi|al.rajhi|rajhi|74100)""")
    private val amountSarPattern = Regex("""(?i)(?:sar|ر\.س|sr)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:sar|ر\.س|sr)""")
    // Handles: "card ending 1234", "Card:7573", "By:7573", "account no. 5678"
    private val cardPattern = Regex("""(?i)(?:card|account|by)[:\s]*(?:ending|no\.?|number)?\s*[xX*]*(\d{4})""")
    private val refPattern = Regex("""(?i)ref(?:erence)?:?\s*(\w+)""")
    // Handles "at Merchant on <date>", "At:Merchant Balance:", "At: Merchant Fee 8VAT:", etc.
    // Stops at known Al Rajhi field names (Fee, Balance, Exchange, Country, Total, Ref, Available)
    // or end of string.
    private val atPattern = Regex("""(?i)\bat[:\s]\s*([A-Za-z0-9][A-Za-z0-9 _\-&./]*?)(?=\s+(?:Fee|Balance|Ref|Exchange|Country|Total|Available|on\s+\d)|\s*${'$'})""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\b(?:purchase|purchased|debited|deducted|خصم|مشتريات)\b""").containsMatchIn(body)
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

        return ParsedTransaction(
            amount = amount,
            currencyCode = "SAR",
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName
        )
    }
}
