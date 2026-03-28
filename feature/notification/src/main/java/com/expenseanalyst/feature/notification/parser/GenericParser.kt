package com.expenseanalyst.feature.notification.parser

/**
 * Generic fallback parser for any unsupported bank SMS.
 * Tries to extract amount + debit/credit direction from common patterns.
 * Always returns last in the [ParserRegistry] chain.
 */
class GenericParser : TransactionParser {

    override val bankName = "Unknown Bank"

    private val amountPattern = Regex(
        """(?i)(?:₹|rs\.?|inr|sar|ر\.س|usd|aed|eur|gbp|sr\.?)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:rs\.?|inr|sar|ر\.س|usd|aed|eur|gbp|sr\.?)"""
    )
    private val currencyPattern = Regex("""(?i)\b(INR|SAR|USD|AED|EUR|GBP)\b""")

    override fun canParse(sender: String, body: String): Boolean = true

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Detect bill/card payment confirmations before generic debit/credit
        val isPayment = Regex("""(?i)\b(?:payment\s+(?:received|successful|confirmed|processed)|received\s+payment|paid\s+(?:your|against|towards)|bill\s+paid|due\s+paid)\b""").containsMatchIn(body)
        val isDebit = !isPayment && Regex("""(?i)\b(?:debited|deducted|paid|purchase|sent|withdrawn)\b""").containsMatchIn(body)
        val isCredit = !isPayment && Regex("""(?i)\b(?:credited|received|refund)\b""").containsMatchIn(body)
        if (!isDebit && !isCredit && !isPayment) return null

        val amountMatch = amountPattern.find(body) ?: return null
        val amount = (amountMatch.groupValues[1].takeIf { it.isNotBlank() }
            ?: amountMatch.groupValues[2])
            .replace(",", "").toDoubleOrNull() ?: return null

        val text = body.uppercase()
        val currencyCode = when {
            text.contains("₹") || text.contains("INR") || text.contains("RS.") || text.contains("RS ") -> "INR"
            text.contains("SAR") || text.contains("ر.س") || text.contains("SR") -> "SAR"
            text.contains("AED") -> "AED"
            text.contains("EUR") -> "EUR"
            text.contains("GBP") -> "GBP"
            text.contains("USD") -> "USD"
            else -> "INR" // default for Indian context
        }

        // Try to extract merchant from "At: Merchant" or "at Merchant" pattern
        val merchant = Regex("""(?i)\bat[:\s]\s*([A-Za-z0-9][A-Za-z0-9 _\-&./]*?)(?=\s*\n|\s+(?:Amount|Fee|Balance|Ref|Exchange|Country|Total|Available)|\s*$)""")
            .find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() && it.length < 60 }

        // Try to extract last-4 from "Card:7573" or "card ending 1234"
        val accountLast4 = Regex("""(?i)(?:card|account|by)[:\s]*(?:ending|no\.?|number)?\s*[xX*]*(\d{4})""")
            .find(body)?.groupValues?.get(1)

        val direction = when {
            isPayment -> TransactionDirection.PAYMENT
            isDebit -> TransactionDirection.DEBIT
            else -> TransactionDirection.CREDIT
        }
        return ParsedTransaction(
            amount = amount,
            currencyCode = currencyCode,
            type = direction,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = null,
            bankName = bankName,
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }
}
