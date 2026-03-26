package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.MerchantRule
import kotlinx.coroutines.flow.Flow

interface MerchantRuleRepository {
    fun getRules(): Flow<List<MerchantRule>>
    suspend fun saveRule(merchantPattern: String, categoryId: Long, categoryName: String)
    suspend fun deleteRule(id: Long)
}
