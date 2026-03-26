package com.expenseanalyst.data.repository

import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.data.local.dao.CategoryDao
import com.expenseanalyst.data.local.dao.EmiGroupDao
import com.expenseanalyst.data.local.dao.ExpenseDao
import com.expenseanalyst.data.local.entity.EmiGroupEntity
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.EmiGroup
import com.expenseanalyst.domain.repository.EmiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmiRepositoryImpl @Inject constructor(
    private val emiGroupDao: EmiGroupDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) : EmiRepository {

    override fun getActiveEmiGroups(): Flow<List<EmiGroup>> =
        emiGroupDao.getAllEmiGroups().map { groups ->
            groups.mapNotNull { entity -> resolveGroup(entity) }
                .filter { it.paidCount < it.numberOfInstallments }
        }

    override fun getCompletedEmiGroups(): Flow<List<EmiGroup>> =
        emiGroupDao.getAllEmiGroups().map { groups ->
            groups.mapNotNull { entity -> resolveGroup(entity) }
                .filter { it.paidCount >= it.numberOfInstallments }
        }

    override fun getEmiGroupById(id: Long): Flow<EmiGroup?> =
        emiGroupDao.getEmiGroupById(id).map { entity ->
            entity?.let { resolveGroup(it) }
        }

    override suspend fun createEmiGroup(emiGroup: EmiGroup): Long {
        val entity = EmiGroupEntity(
            totalAmount = emiGroup.totalAmount,
            currencyCode = emiGroup.currencyCode,
            numberOfInstallments = emiGroup.numberOfInstallments,
            installmentAmount = emiGroup.installmentAmount,
            interestRate = emiGroup.interestRate,
            startDateUtcMillis = emiGroup.startDate.toEpochMilliseconds(),
            description = emiGroup.description,
            categoryId = emiGroup.category.id,
            paymentMethod = emiGroup.paymentMethod.name,
            createdAtUtcMillis = DateTimeUtil.nowMillis()
        )
        return emiGroupDao.insertEmiGroup(entity)
    }

    override suspend fun cancelRemainingInstallments(emiGroupId: Long) {
        val nowMillis = DateTimeUtil.nowMillis()
        expenseDao.softDeleteFutureEmiInstallments(
            emiGroupId = emiGroupId,
            afterMillis = nowMillis,
            updatedAt = nowMillis
        )
    }

    private suspend fun resolveGroup(entity: EmiGroupEntity): EmiGroup? {
        val categoryEntity = categoryDao.getCategoryById(entity.categoryId).first() ?: return null
        val category = categoryEntity.toDomain()
        val paidCount = expenseDao.countPaidInstallments(entity.id, DateTimeUtil.nowMillis())
        return entity.toDomain(category, paidCount)
    }
}

private fun EmiGroupEntity.toDomain(category: Category, paidCount: Int): EmiGroup = EmiGroup(
    id = id,
    totalAmount = totalAmount,
    currencyCode = currencyCode,
    numberOfInstallments = numberOfInstallments,
    installmentAmount = installmentAmount,
    interestRate = interestRate,
    startDate = kotlinx.datetime.Instant.fromEpochMilliseconds(startDateUtcMillis),
    description = description,
    category = category,
    paymentMethod = com.expenseanalyst.domain.model.PaymentMethod.valueOf(paymentMethod),
    paidCount = paidCount
)
