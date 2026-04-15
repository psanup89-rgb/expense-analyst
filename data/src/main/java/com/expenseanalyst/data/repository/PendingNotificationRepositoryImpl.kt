package com.expenseanalyst.data.repository

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.expenseanalyst.data.local.dao.PendingNotificationDao
import com.expenseanalyst.data.local.entity.PendingNotificationEntity
import com.expenseanalyst.domain.model.PendingNotification
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PendingNotificationRepositoryImpl @Inject constructor(
    private val dao: PendingNotificationDao,
    @ApplicationContext private val context: Context
) : PendingNotificationRepository {

    override fun getAll(): Flow<List<PendingNotification>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getCount(): Flow<Int> = dao.getCount()

    override suspend fun getById(id: Long): PendingNotification? = dao.getById(id)?.toDomain()

    override suspend fun save(notification: PendingNotification): Long =
        dao.insert(notification.toEntity())

    override suspend fun findRecentByRawBody(rawBody: String, sinceMillis: Long): PendingNotification? =
        dao.findRecentByRawBody(rawBody, sinceMillis)?.toDomain()

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
        // Cancel the system tray notification whose ID equals the pending notification's DB id.
        // TransactionAlertNotification.post() uses pendingId.toInt() as the Android notification ID.
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }

    override suspend fun deleteAll() = dao.deleteAll()

    private fun PendingNotificationEntity.toDomain() = PendingNotification(
        id = id,
        amount = amount,
        currencyCode = currencyCode,
        merchantName = merchantName,
        bankName = bankName,
        accountLast4 = accountLast4,
        transactionType = transactionType,
        detectedAtMillis = detectedAtMillis,
        rawBody = rawBody,
        paymentMethod = paymentMethod,
        isPossibleDuplicate = isPossibleDuplicate,
        pendingType = pendingType,
        billerName = billerName,
        dueDateMillis = dueDateMillis,
        linkedBillId = linkedBillId
    )

    private fun PendingNotification.toEntity() = PendingNotificationEntity(
        id = id,
        amount = amount,
        currencyCode = currencyCode,
        merchantName = merchantName,
        bankName = bankName,
        accountLast4 = accountLast4,
        transactionType = transactionType,
        detectedAtMillis = detectedAtMillis,
        rawBody = rawBody,
        paymentMethod = paymentMethod,
        isPossibleDuplicate = isPossibleDuplicate,
        pendingType = pendingType,
        billerName = billerName,
        dueDateMillis = dueDateMillis,
        linkedBillId = linkedBillId
    )
}
