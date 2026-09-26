package com.expenseanalyst.feature.notification.service

/**
 * The words on a transaction notification, kept apart from the Android builder so they can be
 * unit-tested. Rules (from the "Ledger — final" notifications board):
 *
 * - The title leads with the money, in the app's own format (`SAR42.00`, never `42.00 SAR`),
 *   then the merchant. Money in carries a leading `+`.
 * - Merchants arrive in raw SMS capitals ("PAUL CAFE"); all-caps names are title-cased for display.
 *   Mixed-case names are the bank's own styling and are left alone.
 * - The second line names the category and the bank — never card or account digits (CLAUDE.md
 *   output rules).
 */
internal object NotificationCopy {

    /** "PAUL CAFE" → "Paul Cafe"; "LULU EXPRESS DIPLOMATIC Q" → "Lulu Express Diplomatic Q". */
    fun displayMerchant(raw: String?): String? {
        val name = raw?.trim()?.replace(WHITESPACE_RUN, " ")?.takeIf { it.isNotEmpty() } ?: return null
        val letters = name.filter { it.isLetter() }
        if (letters.isEmpty() || letters != letters.uppercase()) return name
        return name.split(" ").joinToString(" ") { word ->
            if (word.length <= 1) word else word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun expenseTitle(formattedAmount: String, merchant: String?, incoming: Boolean): String {
        val amount = if (incoming) "+$formattedAmount" else formattedAmount
        return displayMerchant(merchant)?.let { "$amount · $it" } ?: amount
    }

    fun expenseBody(categoryName: String, bankName: String?, needsReview: Boolean): String {
        if (needsReview) return "Needs review · tap to finish"
        val bank = bankName?.takeIf { it.isNotBlank() && it != UNKNOWN_BANK }
        return if (bank != null) "$categoryName · $bank" else categoryName
    }

    fun monthToDateLine(monthName: String, formattedTotal: String): String =
        "$monthName so far: $formattedTotal spent"

    private const val UNKNOWN_BANK = "Unknown Bank"
    private val WHITESPACE_RUN = Regex("""\s+""")
}
