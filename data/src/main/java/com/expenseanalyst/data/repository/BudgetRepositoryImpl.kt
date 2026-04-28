package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.ExpenseDao
import com.expenseanalyst.data.local.dao.PlannedExpenseDao
import com.expenseanalyst.data.local.dao.SalaryDao
import com.expenseanalyst.data.local.entity.PlannedExpenseEntity
import com.expenseanalyst.data.local.entity.SalaryEntryEntity
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PlannedExpense
import com.expenseanalyst.domain.model.SalaryEntry
import com.expenseanalyst.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val salaryDao: SalaryDao,
    private val plannedExpenseDao: PlannedExpenseDao,
    private val expenseDao: ExpenseDao
) : BudgetRepository {

    override fun getSalary(month: Int, year: Int): Flow<SalaryEntry?> =
        salaryDao.getByMonthYear(month, year).map { it?.toDomain() }

    override fun getSalaryHistory(): Flow<List<SalaryEntry>> =
        salaryDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveSalary(entry: SalaryEntry): Long =
        salaryDao.upsert(entry.toEntity())

    override suspend fun deleteSalary(month: Int, year: Int) =
        salaryDao.delete(month, year)

    override fun getPlannedExpenses(month: Int, year: Int): Flow<List<PlannedExpense>> =
        plannedExpenseDao.getByMonthYear(month, year).map { list -> list.map { it.toDomain() } }

    override suspend fun savePlannedExpense(item: PlannedExpense): Long =
        plannedExpenseDao.insert(item.toEntity())

    override suspend fun updatePlannedExpense(item: PlannedExpense) =
        plannedExpenseDao.update(item.toEntity())

    override suspend fun softDeletePlannedExpense(id: Long) =
        plannedExpenseDao.softDelete(id)

    override suspend fun carryForward(fromMonth: Int, fromYear: Int, toMonth: Int, toYear: Int) {
        val source = plannedExpenseDao.getByMonthYearSnapshot(fromMonth, fromYear)
        val now = System.currentTimeMillis()
        source.forEach { item ->
            plannedExpenseDao.insert(
                item.copy(id = 0, month = toMonth, year = toYear, createdAtMillis = now)
            )
        }
    }

    override fun getIncomeTransactions(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        expenseDao.getIncomeByDateRange(startMillis, endMillis)
            .map { list -> list.map { it.toDomain() } }

    private fun SalaryEntryEntity.toDomain() = SalaryEntry(
        id = id, amount = amount, currencyCode = currencyCode,
        month = month, year = year, sourceExpenseId = sourceExpenseId,
        isConfirmed = isConfirmed, createdAtMillis = createdAtMillis
    )

    private fun SalaryEntry.toEntity() = SalaryEntryEntity(
        id = id, amount = amount, currencyCode = currencyCode,
        month = month, year = year, sourceExpenseId = sourceExpenseId,
        isConfirmed = isConfirmed, createdAtMillis = createdAtMillis
    )

    private fun PlannedExpenseEntity.toDomain() = PlannedExpense(
        id = id, description = description, amount = amount,
        currencyCode = currencyCode, categoryId = categoryId,
        month = month, year = year, isDeleted = isDeleted,
        createdAtMillis = createdAtMillis
    )

    private fun PlannedExpense.toEntity() = PlannedExpenseEntity(
        id = id, description = description, amount = amount,
        currencyCode = currencyCode, categoryId = categoryId,
        month = month, year = year, isDeleted = isDeleted,
        createdAtMillis = createdAtMillis
    )
}
