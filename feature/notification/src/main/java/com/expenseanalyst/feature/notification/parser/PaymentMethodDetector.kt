package com.expenseanalyst.feature.notification.parser

/**
 * Infers the PaymentMethod enum name from SMS body text.
 *
 * Priority order (highest wins):
 * 1. Wallet pay (Apple Pay, Samsung Pay, Google Pay) — these are overlaid on a card
 * 2. UPI — "via UPI", "UPI/P2M", "UPI Ref", "UPI:" etc.
 * 3. Net Banking — "NEFT", "IMPS", "RTGS", "Net Banking", "BBPS"
 * 4. Credit Card — "Credit Card", "CC no", "card" in credit-card context
 * 5. Debit Card — "Debit Card", "Fx Card", "Forex Card"
 * 6. Savings/Current account fallback — treated as DEBIT_CARD (bank transfer from account)
 *
 * Returns a PaymentMethod enum name string (e.g. "CREDIT_CARD") or null if unknown.
 */
object PaymentMethodDetector {

    private val applePayPattern = Regex("""(?i)\bapple\s*pay\b""")
    private val samsungPayPattern = Regex("""(?i)\bsamsung\s*pay\b""")
    private val googlePayPattern = Regex("""(?i)\bgoogle\s*pay\b""")
    private val upiPattern = Regex("""(?i)\b(?:UPI|UPI/P2[MA]|UPI\s*Ref)\b|UPI:""")
    private val netBankingPattern = Regex("""(?i)\b(?:NEFT|IMPS|RTGS|Net\s*Banking|BBPS|ACH\s+D-)\b""")
    private val creditCardPattern = Regex("""(?i)\b(?:credit\s*card|CC\s*no)\b""")
    private val debitCardPattern = Regex("""(?i)\b(?:debit\s*card|Fx\s*Card|Forex\s*Card)\b""")

    fun detect(body: String): String? {
        // 1. Wallet overlays (highest priority — these ride on top of a card)
        if (applePayPattern.containsMatchIn(body)) return "APPLE_PAY"
        if (samsungPayPattern.containsMatchIn(body)) return "SAMSUNG_PAY"
        if (googlePayPattern.containsMatchIn(body)) return "GOOGLE_PAY"

        // 2. UPI
        if (upiPattern.containsMatchIn(body)) return "UPI"

        // 3. Net Banking / NEFT / IMPS
        if (netBankingPattern.containsMatchIn(body)) return "NET_BANKING"

        // 4. Credit Card
        if (creditCardPattern.containsMatchIn(body)) return "CREDIT_CARD"

        // 5. Debit Card / Forex Card
        if (debitCardPattern.containsMatchIn(body)) return "DEBIT_CARD"

        return null
    }
}
