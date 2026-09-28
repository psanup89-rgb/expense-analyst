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
    /** A saved bill (any status) for this biller and amount, created after [sinceMillis]. */
    suspend fun findRecentBillByBillerAndAmount(billerName: String, amount: Double, sinceMillis: Long): Bill?
    /** Adds [count] linked reminders to a bill (targeted update, not a full-row write). */
    suspend fun addReminders(billId: Long, count: Int, atMillis: Long)
}
