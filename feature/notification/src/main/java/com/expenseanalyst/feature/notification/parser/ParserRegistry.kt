package com.expenseanalyst.feature.notification.parser

/**
 * Dispatches incoming notification/SMS to the first matching [TransactionParser].
 * Order matters: specific bank parsers first, generic fallback last.
 */
object ParserRegistry {

    private val parsers: List<TransactionParser> = listOf(
        // Indian banks
        HdfcParser(),
        SbiParser(),
        IciciParser(),
        AxisParser(),
        KotakParser(),
        YesBankParser(),
        IdfcFirstBankParser(),
        OneCardParser(),
        // Saudi banks
        AlRajhiParser(),
        StcBankParser(),
        AlinmaParser(),
        D360Parser(),
        // UAE banks
        EmiratesNbdParser(),
        // Toll / FASTag
        FasTagParser(),
        // Digital wallets
        WalletParser(),
        // UPI apps
        UpiParser(),
        // Bill payment apps (body-fingerprint detection)
        MubasherParser(),
        // Merchant apps (food delivery, e-commerce, etc.)
        KeetaParser(),
        // BNPL purchase-split confirmations (Tabby, Tamara) — see TabbyTamaraParser's KDoc,
        // unverified against a real message. Must precede GenericParser, whose isPayment
        // regex ("payment successful/confirmed") could otherwise catch these SMS first and
        // silently under-count them instead of routing to the BNPL-specific handling.
        TabbyTamaraParser(),
        // Generic fallback
        GenericParser()
    )

    // OTP/verification-code messages often restate the transaction amount for context
    // ("Enter OTP 1234 to authorize SAR 649.00 at noon") but are not themselves a
    // transaction — a bank sends one alongside its real purchase-confirmation SMS. Without
    // this guard, several parsers' generic amount+keyword matching happily parses these as
    // a second, duplicate expense for the same real-world purchase.
    private val otpPattern = Regex("""(?i)\bOTP\b|\bone[\s-]?time[\s-]?password\b|\bverification\s+code\b""")

    /**
     * Finds the first parser that can handle this sender/body, runs it, and returns the result.
     * Returns null only if even the generic parser found nothing useful.
     */
    fun parse(sender: String, body: String): ParsedTransaction? {
        if (otpPattern.containsMatchIn(body)) return null
        for (parser in parsers) {
            if (parser.canParse(sender, body)) {
                val result = parser.parse(sender, body)
                if (result != null) return result
            }
        }
        return null
    }
}
