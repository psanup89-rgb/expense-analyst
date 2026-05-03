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

    // Bill reminders / payment-due notices — not real transactions.
    // These contain "ignore if paid", "minimum amount due", "bill of Rs.X is pending", etc.
    private val billReminderPattern = Regex(
        """(?i)(?:ignore\s+if\s+(?:already\s+)?paid|if\s+(?:already\s+)?paid[,\s]+(?:please\s+)?ignore|bill\s+(?:of|amount).{0,60}(?:pending|is\s+due)|(?:amount|payment)\s+is\s+due\s+on\s+\d|minimum\s+(?:amount\s+)?due|bill\s+payment\s+reminder|payment\s+(?:overdue|reminder)|bill.{0,70}has\s+been\s+generated)"""
    )

    override fun canParse(sender: String, body: String): Boolean = true

    override fun parse(sender: String, body: String): ParsedTransaction? {
        // Bill reminders / due notices — skip entirely, let BillStatementParserRegistry handle them
        if (billReminderPattern.containsMatchIn(body)) return null

        // Detect bill/card payment confirmations before generic debit/credit
        val isPayment = Regex("""(?i)\b(?:payment\s+(?:received|successful|confirmed|processed)|received\s+payment|paid\s+(?:your|against|towards)|bill\s+paid|due\s+paid)\b""").containsMatchIn(body)
        // Strong debit signals are unambiguous ("debited/deducted/withdrawn").
        // Weak debit signals ("paid/sent/purchase") can appear in non-transactional contexts
        // (e.g. "email sent to ...") and must not override an explicit CREDIT signal.
        val isStrongDebit = !isPayment && Regex("""(?i)\b(?:debited|deducted|withdrawn)\b""").containsMatchIn(body)
        val isWeakDebit = !isPayment && !isStrongDebit && Regex("""(?i)\b(?:paid|purchase|sent)\b""").containsMatchIn(body)
        val isDebit = isStrongDebit || isWeakDebit
        val isCredit = !isPayment && Regex("""(?i)\b(?:credited|received|refund(?:ed|s)?)\b""").containsMatchIn(body)
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
            isStrongDebit -> TransactionDirection.DEBIT  // "debited/deducted/withdrawn" always wins
            isCredit -> TransactionDirection.CREDIT       // "credited/refund" beats "paid/sent"
            else -> TransactionDirection.DEBIT            // "paid/sent/purchase" alone → DEBIT
        }
        return ParsedTransaction(
            amount = amount,
            currencyCode = currencyCode,
            type = direction,
            merchant = merchant,
            accountLast4 = accountLast4,
            referenceNumber = null,
            bankName = bankNameFromSender(sender),
            paymentMethodName = PaymentMethodDetector.detect(body)
        )
    }

    /**
     * Attempts to derive a human-readable bank name from the SMS sender ID.
     * Falls back to "Unknown Bank" if no pattern matches.
     * Mirrors the same lookup in SmsImportViewModel.bankDisplayNameFromSender().
     */
    private fun bankNameFromSender(sender: String): String {
        val s = sender.uppercase()
        return when {
            "HDFC" in s -> "HDFC Bank"
            "ICICI" in s -> "ICICI Bank"
            "AXISBK" in s || "AXISBANK" in s -> "Axis Bank"
            "SBIINB" in s || "SBIPSG" in s || "SBIUPI" in s || "SBI" in s -> "SBI"
            "KOTAK" in s -> "Kotak Bank"
            "YESBNK" in s || "YESBANK" in s -> "Yes Bank"
            "INDUS" in s -> "IndusInd Bank"
            "PNBSMS" in s || "PUNJAB" in s -> "PNB"
            "ALRJHI" in s || "ALRAJHI" in s -> "Al Rajhi Bank"
            "ALINMA" in s -> "Alinma Bank"
            "STCBNK" in s || "STCPAY" in s -> "STC Bank"
            "D360" in s -> "Bank D·360"
            "EMIRNBD" in s || "ENBD" in s -> "Emirates NBD"
            "IDFCFB" in s || "IDFCFIRST" in s -> "IDFC First Bank"
            "ONECARD" in s || "FEDERAL" in s -> "OneCard"
            "CANARA" in s -> "Canara Bank"
            "BOB" in s || "BANKOFBARODA" in s -> "Bank of Baroda"
            "UNION" in s -> "Union Bank"
            "CITI" in s -> "Citi Bank"
            "AMEX" in s -> "American Express"
            "PAYTM" in s -> "Paytm"
            "AIRTEL" in s -> "Airtel Payments Bank"
            "CLRTRP" in s || "CLEARTRIP" in s -> "Cleartrip"
            else -> bankName // "Unknown Bank"
        }
    }
}
