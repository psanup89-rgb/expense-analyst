package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.TransferRecipientRuleDao
import com.expenseanalyst.data.local.entity.TransferRecipientRuleEntity
import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.model.TransferRecipientRule
import com.expenseanalyst.domain.repository.TransferRecipientRuleRepository
import com.expenseanalyst.domain.util.TransferRecipientMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransferRecipientRuleRepositoryImpl @Inject constructor(
    private val dao: TransferRecipientRuleDao
) : TransferRecipientRuleRepository {

    override fun getRules(): Flow<List<TransferRecipientRule>> =
        dao.getAll().map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override suspend fun saveRule(recipientName: String, classification: TransferClassification) {
        val key = TransferRecipientMatcher.normalize(recipientName) ?: return
        dao.upsert(
            TransferRecipientRuleEntity(
                recipientKey = key,
                recipientDisplayName = recipientName.trim(),
                classification = classification.name,
                createdAtUtcMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteRule(recipientName: String) {
        val key = TransferRecipientMatcher.normalize(recipientName) ?: return
        dao.deleteByKey(key)
    }

    /**
     * Drops a row whose classification string no longer maps to a known enum constant rather than
     * throwing — same tolerance as the expense mapper, so a downgrade leaves rules inert instead
     * of crashing every screen that reads them.
     */
    private fun TransferRecipientRuleEntity.toDomainOrNull(): TransferRecipientRule? {
        val parsed = runCatching { TransferClassification.valueOf(classification) }.getOrNull()
            ?: return null
        return TransferRecipientRule(
            id = id,
            recipientKey = recipientKey,
            recipientDisplayName = recipientDisplayName,
            classification = parsed,
            createdAt = createdAtUtcMillis
        )
    }
}
