package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.MerchantRuleDao
import com.expenseanalyst.data.local.entity.MerchantRuleEntity
import com.expenseanalyst.data.local.entity.TagEntity
import com.expenseanalyst.data.local.relation.MerchantRuleWithTags
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MerchantRuleRepositoryImpl @Inject constructor(
    private val dao: MerchantRuleDao
) : MerchantRuleRepository {

    override fun getRules(): Flow<List<MerchantRule>> =
        dao.getAllWithTags().map { list -> list.map { it.toDomain() } }

    override suspend fun saveRule(merchantPattern: String, categoryId: Long, categoryName: String, tagIds: List<Long>) {
        val ruleId = dao.upsert(
            MerchantRuleEntity(
                merchantPattern = merchantPattern,
                categoryId = categoryId,
                categoryName = categoryName,
                createdAtUtcMillis = System.currentTimeMillis()
            )
        )
        dao.setTagsForRule(ruleId, tagIds)
    }

    override suspend fun deleteRule(id: Long) = dao.deleteById(id)

    private fun MerchantRuleWithTags.toDomain() = MerchantRule(
        id = rule.id,
        merchantPattern = rule.merchantPattern,
        categoryId = rule.categoryId,
        categoryName = rule.categoryName,
        createdAt = rule.createdAtUtcMillis,
        tags = tags.map { it.toDomain() }
    )

    private fun TagEntity.toDomain() = Tag(id = id, name = name)
}
