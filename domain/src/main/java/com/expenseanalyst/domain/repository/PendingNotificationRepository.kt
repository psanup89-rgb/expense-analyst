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
    suspend fun findRecentBillByBillerAndAmount(billerName: String, amount: Double, sinceMillis: Long): PendingNotification?
    /** The inbox card already collecting reminders for [billId], if any. */
    suspend fun findReminderCardForBill(billId: Long): PendingNotification?
    /** One more reminder on a queued BILL or BILL_REMINDER card. */
    suspend fun addReminder(id: Long)
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}
