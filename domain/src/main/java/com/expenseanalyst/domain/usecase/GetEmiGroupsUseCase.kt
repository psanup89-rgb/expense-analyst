package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.EmiGroup
import com.expenseanalyst.domain.repository.EmiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEmiGroupsUseCase @Inject constructor(
    private val emiRepository: EmiRepository
) {
    fun active(): Flow<List<EmiGroup>> = emiRepository.getActiveEmiGroups()
    fun completed(): Flow<List<EmiGroup>> = emiRepository.getCompletedEmiGroups()
    fun byId(id: Long): Flow<EmiGroup?> = emiRepository.getEmiGroupById(id)
}
