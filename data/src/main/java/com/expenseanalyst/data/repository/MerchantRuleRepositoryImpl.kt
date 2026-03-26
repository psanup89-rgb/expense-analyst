package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.MerchantRuleDao
import com.expenseanalyst.data.local.entity.MerchantRuleEntity
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MerchantRuleRepositoryImpl @Inject constructor(
    private val dao: MerchantRuleDao
) : MerchantRuleRepository {

    override fun getRules(): Flow<List<MerchantRule>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveRule(merchantPattern: String, categoryId: Long, categoryName: String) {
        dao.upsert(
            MerchantRuleEntity(
                merchantPattern = merchantPattern,
                categoryId = categoryId,
                categoryName = categoryName,
                createdAtUtcMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteRule(id: Long) = dao.deleteById(id)

    private fun MerchantRuleEntity.toDomain() = MerchantRule(
        id = id,
        merchantPattern = merchantPattern,
        categoryId = categoryId,
        categoryName = categoryName,
        createdAt = createdAtUtcMillis
    )
}
