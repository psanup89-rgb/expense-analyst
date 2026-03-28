package com.expenseanalyst.feature.notification.parser

/**
 * Axis Bank SMS parser.
 * Sender IDs: AXISBK, AXISBN, AD-AXISBK
 *
 * Sample (INR debit via UPI):
 *   "Rs.600.00 debited from Axis Bank Acct XX9876 on 01-Jan-25. Trf to Zomato via UPI. Ref:987654321"
 * Sample (INR credit):
 *   "Rs.2000.00 credited to Axis Bank Acct XX9876. Sender: John Doe. Ref:123456789"
 * Sample (Forex card, SAR):
 *   "Debited SAR 43.05 from Axis Bank Fx Card XX9665 on 26-03-2026 02:18:34 IST at Keemart. Bal: SAR 5318.50."
 * Sample (UPI debit, compact):
 *   "INR 4386.00 debited A/c no. XX0426 10-03-26, 02:47:02 UPI/P2M/600656614974/3FIVE8 TECHNOLOGIES Not you?"
 */
class AxisParser : TransactionParser {

    override val bankName = "Axis Bank"

    private val senderPattern = Regex("""(?i)axis""")
    // Matches both "Rs.600.00" (INR) and "SAR 43.05" / "USD 10.00" (forex)
    private val amountPattern = Regex("""(?i)(?:rs\.?|inr|sar|usd|aed|eur|gbp)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:rs\.?|inr|sar|usd|aed|eur|gbp)""")
    // Matches "Acct XX9876", "Fx Card XX9665", "Card XX9876", "A/c no. XX0426", "CC no. XX4502"
    private val accountPattern = Regex("""(?i)(?:acct|card|a/c|cc)\s*(?:no\.?)?\s*[xX*\s]+(\d{3,4})""")
    private val refPattern = Regex("""(?i)ref:?\s*(\w+)""")
    // Merchant from UPI transfer: "Trf to Zomato via UPI"
    private val transferToPattern = Regex("""(?i)(?:trf\s*to|transfer\s*to)\s+([A-Za-z0-9 _\-&.@]+?)(?:\s*(?:via|ref|on\s+\d|\.)|$)""")
    // Merchant from POS/forex: "at Keemart." or "at Extra Stores on"
    private val atPattern = Regex("""(?i)\bat\s+([A-Za-z0-9][A-Za-z0-9 _\-&./]*?)(?=\s+(?:Bal|Ref|on\s+\d|via)|\.\s*(?:Bal|${'$'})|\s*${'$'})""")
    // Merchant from UPI compact format: "UPI/P2M/txnid/MERCHANT NAME Not you?"
    private val upiMerchantPattern = Regex("""UPI/P2[MA]/\d+/([A-Za-z0-9][A-Za-z0-9 _\-&.]*?)(?=\s+Not\s|\s*$)""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val isDebit = Regex("""(?i)\bdebited\b""").containsMatchIn(body)
        val isCredit = Regex("""(?i)\bcredited\b""").containsMatchIn(body)
        if (!isDebit && !isCredit) return null

        val amountMatch = amountPattern.find(body)
        val amount = (amountMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: amountMatch?.groupValues?.get(2)?.takeIf { it.isNotBlank() })
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        // Detect currency from SMS body
        val bodyUpper = body.uppercase()
        val currencyCode = when {
            bodyUpper.contains("SAR") || bodyUpper.contains("ر.س") -> "SAR"
            bodyUpper.contains("AED") -> "AED"
            bodyUpper.contains("USD") -> "USD"
            bodyUpper.contains("EUR") -> "EUR"
            bodyUpper.contains("GBP") -> "GBP"
            else -> "INR"
        }

        val accountLast4 = accountPattern.find(body)?.groupValues?.get(1)
        val ref = refPattern.find(body)?.groupValues?.get(1)
        // Prefer UPI transfer target, then POS "at Merchant", then UPI compact format
        val merchant = (transferToPattern.find(body)?.groupValues?.get(1)
            ?: atPattern.find(body)?.groupValues?.get(1)
            ?: upiMerchantPattern.find(body)?.groupValues?.get(1))
            ?.trim()?.takeIf { it.isNotBlank() && it.length < 60 }

        return ParsedTransaction(
            amount = amount,
            currencyCode = currencyCode,
            type = if (isDebit) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = ref,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
