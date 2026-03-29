package com.expenseanalyst.domain.repository

/**
 * Searches the web to identify a merchant's business type and maps it to a category name.
 * Used as Tier 3 fallback when keyword matching fails.
 */
interface MerchantSearchRepository {
    /**
     * Returns the inferred category name (e.g. "Food", "Shopping") for the given merchant,
     * or null if the merchant could not be identified.
     * This is a network call — must be called from a coroutine.
     */
    suspend fun searchMerchantCategory(merchantName: String): String?
}
