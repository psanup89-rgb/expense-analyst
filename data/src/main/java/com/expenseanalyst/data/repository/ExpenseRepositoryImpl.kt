package com.expenseanalyst.data.repository

import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.data.local.dao.ExpenseDao
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.data.mapper.toEntity
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getExpenses(): Flow<List<Expense>> =
        expenseDao.getAllExpensesWithCategory().map { list -> list.map { it.toDomain() } }

    override fun getExpensesByDateRange(start: Instant, end: Instant): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRangeWithCategory(
            startMillis = start.toEpochMilliseconds(),
            endMillis = end.toEpochMilliseconds()
        ).map { list -> list.map { it.toDomain() } }

    override fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCategoryWithCategory(categoryId)
            .map { list -> list.map { it.toDomain() } }

    override fun getExpenseById(id: Long): Flow<Expense?> =
        expenseDao.getExpenseByIdWithCategory(id).map { it?.toDomain() }

    override fun getExpensesByEmiGroup(emiGroupId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByEmiGroupWithCategory(emiGroupId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getExpensesSnapshot(includeDeleted: Boolean): List<Expense> {
        val expenses = if (includeDeleted) {
            expenseDao.getAllExpensesWithCategorySnapshot()
        } else {
            expenseDao.getActiveExpensesWithCategorySnapshot()
        }
        return expenses.map { it.toDomain() }
    }

    override suspend fun addExpense(expense: Expense): Long {
        val now = DateTimeUtil.nowMillis()
        return expenseDao.insertExpense(expense.toEntity(createdAt = now, updatedAt = now))
    }

    override suspend fun addExpenses(expenses: List<Expense>) {
        val now = DateTimeUtil.nowMillis()
        expenseDao.insertAll(expenses.map { it.toEntity(createdAt = now, updatedAt = now) })
    }

    override suspend fun updateExpense(expense: Expense) {
        val now = DateTimeUtil.nowMillis()
        val existing = expenseDao.getExpenseEntityById(expense.id)
        val createdAt = existing?.createdAtUtcMillis ?: now
        expenseDao.updateExpense(expense.toEntity(createdAt = createdAt, updatedAt = now))
    }

    override suspend fun softDeleteExpense(id: Long) {
        expenseDao.softDelete(id = id, updatedAt = DateTimeUtil.nowMillis())
    }

    override suspend fun restoreExpense(id: Long) {
        expenseDao.restore(id = id, updatedAt = DateTimeUtil.nowMillis())
    }

    override fun getExpensesByBillId(billId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByBillId(billId).map { list -> list.map { it.toDomain() } }
}
