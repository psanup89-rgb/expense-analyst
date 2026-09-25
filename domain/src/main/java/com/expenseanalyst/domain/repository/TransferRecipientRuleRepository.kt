package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.model.TransferRecipientRule
import kotlinx.coroutines.flow.Flow

interface TransferRecipientRuleRepository {
    fun getRules(): Flow<List<TransferRecipientRule>>

    /**
     * Upserts the rule for [recipientName]. No-ops when the name normalises to null (blank), so
     * callers don't have to guard. Re-classifying the same recipient replaces the old rule.
     */
    suspend fun saveRule(recipientName: String, classification: TransferClassification)

    suspend fun deleteRule(recipientName: String)
}
