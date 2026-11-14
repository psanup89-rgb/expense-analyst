package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense

/**
 * Rules behind "Set category" on a Review card (Nov 2026): which other Review rows go with this
 * one, whether a merchant rule is safe to save, and which categories to offer first.
 */
object ReviewCategoryPlanner {

    /** The row asks for a category and the card should show the button. */
    fun needsCategory(e: Expense): Boolean = ReviewReason.GENERIC_CATEGORY in e.reviewReasons

    /**
     * A merchant rule keys on the merchant NAME, so it is only safe when the bank gave one. A row
     * flagged MISSING_MERCHANT carries the bank's own name ("HDFC Bank") as a stand-in — a rule on
     * that would categorise every message from the bank.
     */
    fun canRememberMerchant(e: Expense): Boolean =
        !e.merchantName.isNullOrBlank() && ReviewReason.MISSING_MERCHANT !in e.reviewReasons

    fun merchantPattern(e: Expense): String? =
        e.merchantName?.trim()?.lowercase()?.takeIf { canRememberMerchant(e) && it.isNotBlank() }

    /** Other Review rows that are waiting for a category from the same merchant. */
    fun sameMerchantRows(picked: Expense, reviewRows: List<Expense>): List<Expense> {
        val pattern = merchantPattern(picked) ?: return emptyList()
        return reviewRows.filter {
            it.id != picked.id && needsCategory(it) && merchantPattern(it) == pattern
        }
    }

    /**
     * Categories to offer: never the generic ones (picking Misc/Other wouldn't answer the
     * question), those this merchant already uses on saved expenses first (most used first),
     * then the rest in the user's own order.
     */
    fun offeredCategories(picked: Expense, categories: List<Category>, allExpenses: List<Expense>): List<Category> {
        val usable = categories.filterNot { NeedsReviewEvaluator.isGenericCategory(it.name) }
        val pattern = merchantPattern(picked)
        val usedByMerchant: Map<Long, Int> = if (pattern == null) emptyMap() else {
            allExpenses
                .filter { !it.isDeleted && it.id != picked.id && it.merchantName?.trim()?.lowercase() == pattern }
                .groupingBy { it.category.id }
                .eachCount()
        }
        return usable.sortedWith(
            compareByDescending<Category> { usedByMerchant[it.id] ?: 0 }.thenBy { it.sortOrder }
        )
    }
}
