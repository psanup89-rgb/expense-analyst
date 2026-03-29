package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.PendingNotification
import kotlinx.coroutines.flow.Flow

interface PendingNotificationRepository {
    fun getAll(): Flow<List<PendingNotification>>
    fun getCount(): Flow<Int>
    suspend fun getById(id: Long): PendingNotification?
    suspend fun save(notification: PendingNotification): Long
    /** Find a pending notification with the same raw body text, detected after [sinceMillis]. */
    suspend fun findRecentByRawBody(rawBody: String, sinceMillis: Long): PendingNotification?
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}
