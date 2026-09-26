package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.MerchantRule
import kotlinx.coroutines.flow.Flow

interface MerchantRuleRepository {
    fun getRules(): Flow<List<MerchantRule>>
    /**
     * Upserts the rule for [merchantPattern].
     *
     * [tagIds] null (the default) **keeps the rule's existing tags**; an explicit list replaces
     * them. The default used to be an empty list, so every caller that only meant to set a
     * category — the Add/Edit screens' category auto-save and SMS import's web-search discovery —
     * silently wiped the rule's tags. Only pass a list when the caller is deliberately editing
     * the rule's tags (the rule dialog on the expense detail screen).
     */
    suspend fun saveRule(merchantPattern: String, categoryId: Long, categoryName: String, tagIds: List<Long>? = null)
    suspend fun deleteRule(id: Long)
}
