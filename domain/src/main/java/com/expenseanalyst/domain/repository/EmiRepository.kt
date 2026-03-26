package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.EmiGroup
import kotlinx.coroutines.flow.Flow

interface EmiRepository {
    fun getActiveEmiGroups(): Flow<List<EmiGroup>>
    fun getCompletedEmiGroups(): Flow<List<EmiGroup>>
    fun getEmiGroupById(id: Long): Flow<EmiGroup?>
    suspend fun createEmiGroup(emiGroup: EmiGroup): Long
    suspend fun cancelRemainingInstallments(emiGroupId: Long)
}
