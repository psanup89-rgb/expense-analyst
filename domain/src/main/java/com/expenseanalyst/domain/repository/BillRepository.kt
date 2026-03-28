package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import kotlinx.coroutines.flow.Flow

interface BillRepository {
    fun getBills(): Flow<List<Bill>>
    fun getBillsByStatus(status: BillStatus): Flow<List<Bill>>
    fun getBillById(id: Long): Flow<Bill?>
    suspend fun saveBill(bill: Bill): Long
    suspend fun updateBill(bill: Bill)
    suspend fun softDeleteBill(id: Long)
    /** Finds an open (PENDING or PARTIAL) bill for the given biller, optionally matching account. */
    suspend fun findOpenBillByBiller(billerName: String, accountId: Long?): Bill?
}
