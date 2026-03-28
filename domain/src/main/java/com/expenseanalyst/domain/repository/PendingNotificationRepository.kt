package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.PendingNotification
import kotlinx.coroutines.flow.Flow

interface PendingNotificationRepository {
    fun getAll(): Flow<List<PendingNotification>>
    fun getCount(): Flow<Int>
    suspend fun getById(id: Long): PendingNotification?
    suspend fun save(notification: PendingNotification): Long
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}
