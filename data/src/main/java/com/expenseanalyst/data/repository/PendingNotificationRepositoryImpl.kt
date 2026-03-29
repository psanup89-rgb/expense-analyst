package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.PendingNotificationDao
import com.expenseanalyst.data.local.entity.PendingNotificationEntity
import com.expenseanalyst.domain.model.PendingNotification
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PendingNotificationRepositoryImpl @Inject constructor(
    private val dao: PendingNotificationDao
) : PendingNotificationRepository {

    override fun getAll(): Flow<List<PendingNotification>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getCount(): Flow<Int> = dao.getCount()

    override suspend fun getById(id: Long): PendingNotification? = dao.getById(id)?.toDomain()

    override suspend fun save(notification: PendingNotification): Long =
        dao.insert(notification.toEntity())

    override suspend fun findRecentByRawBody(rawBody: String, sinceMillis: Long): PendingNotification? =
        dao.findRecentByRawBody(rawBody, sinceMillis)?.toDomain()

    override suspend fun delete(id: Long) = dao.deleteById(id)

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
        paymentMethod = paymentMethod
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
        paymentMethod = paymentMethod
    )
}
