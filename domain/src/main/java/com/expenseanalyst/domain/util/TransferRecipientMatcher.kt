package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.TransferRecipientRule

/**
 * Single source of truth for "does this recipient name match a remembered transfer rule".
 *
 * Intentionally NOT [MerchantRuleMatcher]. That matcher is case-insensitive *substring
 * containment*, which is right for merchant brands ("AMAZON" matching "Amazon SA") and actively
 * dangerous for person names: a rule stored as "RAJ" would silently claim "RAJASEKAR", "RAJESH"
 * and "SIVARAJ", so classifying one person's transfers would misclassify several others'.
 * Matching here is exact equality on a normalised key.
 */
object TransferRecipientMatcher {

    /**
     * Normalises a recipient name into a match key: trimmed, upper-cased, with runs of internal
     * whitespace collapsed to a single space. Returns null for a null or blank name, which means
     * "cannot be keyed" — a transfer with no recipient name can never match or create a rule.
     */
    fun normalize(name: String?): String? =
        name?.trim()?.replace(WHITESPACE_RUN, " ")?.uppercase()?.takeIf { it.isNotEmpty() }

    fun findRule(
        recipientName: String?,
        rules: List<TransferRecipientRule>
    ): TransferRecipientRule? {
        val key = normalize(recipientName) ?: return null
        return rules.firstOrNull { it.recipientKey == key }
    }

    private val WHITESPACE_RUN = Regex("""\s+""")
}
