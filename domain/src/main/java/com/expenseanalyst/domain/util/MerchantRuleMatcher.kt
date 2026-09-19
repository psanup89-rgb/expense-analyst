package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.MerchantRule

/**
 * Single source of truth for "does this merchant string match this rule's pattern" — a
 * case-insensitive substring containment check. Previously duplicated independently in
 * [CategoryInference], ExpenseDetailViewModel's existing-rule lookup, and the notification
 * auto-capture pipeline; consolidated here so all call sites agree on the same match semantics.
 */
object MerchantRuleMatcher {

    fun findMatch(merchant: String?, rules: List<MerchantRule>): MerchantRule? {
        if (merchant.isNullOrBlank() || rules.isEmpty()) return null
        val ml = merchant.lowercase()
        return rules.firstOrNull { ml.contains(it.merchantPattern.lowercase()) }
    }
}
