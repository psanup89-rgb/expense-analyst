package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.BillDao
import com.expenseanalyst.data.local.entity.BillEntity
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BillRepositoryImpl @Inject constructor(
    private val billDao: BillDao
) : BillRepository {

    override fun getBills(): Flow<List<Bill>> =
        billDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getBillsByStatus(status: BillStatus): Flow<List<Bill>> =
        billDao.getByStatus(status.name).map { list -> list.map { it.toDomain() } }

    override fun getBillById(id: Long): Flow<Bill?> =
        billDao.getById(id).map { it?.toDomain() }

    override suspend fun saveBill(bill: Bill): Long =
        billDao.insert(bill.toEntity())

    override suspend fun updateBill(bill: Bill) =
        billDao.update(bill.toEntity())

    override suspend fun softDeleteBill(id: Long) =
        billDao.softDelete(id)

    override suspend fun findOpenBillByBiller(billerName: String, accountId: Long?): Bill? =
        billDao.findOpenByBiller(billerName, accountId)?.toDomain()

    private fun BillEntity.toDomain() = Bill(
        id = id,
        billerName = billerName,
        accountId = accountId,
        totalDue = totalDue,
        minimumDue = minimumDue,
        currencyCode = currencyCode,
        dueDateMillis = dueDateMillis,
        statementPeriodStart = statementPeriodStart,
        statementPeriodEnd = statementPeriodEnd,
        status = BillStatus.valueOf(status),
        sourceType = SourceType.valueOf(sourceType),
        createdAtMillis = createdAtMillis,
        isDeleted = isDeleted
    )

    private fun Bill.toEntity() = BillEntity(
        id = id,
        billerName = billerName,
        accountId = accountId,
        totalDue = totalDue,
        minimumDue = minimumDue,
        currencyCode = currencyCode,
        dueDateMillis = dueDateMillis,
        statementPeriodStart = statementPeriodStart,
        statementPeriodEnd = statementPeriodEnd,
        status = status.name,
        sourceType = sourceType.name,
        createdAtMillis = createdAtMillis,
        isDeleted = isDeleted
    )
}
