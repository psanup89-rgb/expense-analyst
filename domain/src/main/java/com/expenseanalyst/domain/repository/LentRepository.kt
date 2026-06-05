package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.LentItem
import kotlinx.coroutines.flow.Flow

interface LentRepository {
    fun getLentItems(includeDeleted: Boolean = false): Flow<List<LentItem>>
    suspend fun getLentItemById(id: Long): LentItem?
    suspend fun addLentItem(item: LentItem): Long
    suspend fun updateLentItem(item: LentItem)
    suspend fun softDeleteLentItem(id: Long)
    suspend fun getLentItemsDueForReminder(nowMillis: Long): List<LentItem>
}
