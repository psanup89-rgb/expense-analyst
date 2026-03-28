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
        // Generic fallback
        GenericParser()
    )

    /**
     * Finds the first parser that can handle this sender/body, runs it, and returns the result.
     * Returns null only if even the generic parser found nothing useful.
     */
    fun parse(sender: String, body: String): ParsedTransaction? {
        for (parser in parsers) {
            if (parser.canParse(sender, body)) {
                val result = parser.parse(sender, body)
                if (result != null) return result
            }
        }
        return null
    }
}
