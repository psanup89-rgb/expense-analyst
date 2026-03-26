package com.expenseanalyst.feature.notification.parser

interface TransactionParser {
    /** Human-readable name for this parser (e.g. "HDFC Bank") */
    val bankName: String

    /**
     * Returns true if this parser can handle the given sender + body.
     * Used by [ParserRegistry] to dispatch.
     */
    fun canParse(sender: String, body: String): Boolean

    /**
     * Attempt to parse the notification text.
     * Returns null if the text doesn't match the expected pattern.
     */
    fun parse(sender: String, body: String): ParsedTransaction?
}
