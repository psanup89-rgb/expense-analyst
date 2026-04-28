package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PlannedExpense
import com.expenseanalyst.domain.model.SalaryEntry
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getSalary(month: Int, year: Int): Flow<SalaryEntry?>
    fun getSalaryHistory(): Flow<List<SalaryEntry>>
    suspend fun saveSalary(entry: SalaryEntry): Long
    suspend fun deleteSalary(month: Int, year: Int)

    fun getPlannedExpenses(month: Int, year: Int): Flow<List<PlannedExpense>>
    suspend fun savePlannedExpense(item: PlannedExpense): Long
    suspend fun updatePlannedExpense(item: PlannedExpense)
    suspend fun softDeletePlannedExpense(id: Long)
    suspend fun carryForward(fromMonth: Int, fromYear: Int, toMonth: Int, toYear: Int)

    fun getIncomeTransactions(startMillis: Long, endMillis: Long): Flow<List<Expense>>
}
