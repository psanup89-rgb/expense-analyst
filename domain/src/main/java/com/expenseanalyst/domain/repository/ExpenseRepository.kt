package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface ExpenseRepository {
    fun getExpenses(): Flow<List<Expense>>
    fun getExpensesByDateRange(start: Instant, end: Instant): Flow<List<Expense>>
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>
    fun getExpenseById(id: Long): Flow<Expense?>
    fun getExpensesByEmiGroup(emiGroupId: Long): Flow<List<Expense>>
    suspend fun getExpensesSnapshot(includeDeleted: Boolean = false): List<Expense>
    suspend fun addExpense(expense: Expense): Long
    suspend fun addExpenses(expenses: List<Expense>)
    suspend fun updateExpense(expense: Expense)
    suspend fun softDeleteExpense(id: Long)
    suspend fun restoreExpense(id: Long)
    fun getExpensesByBillId(billId: Long): Flow<List<Expense>>
    suspend fun countByAccount(accountId: Long): Int
    suspend fun getExpensesByAccount(accountId: Long): List<Expense>
    suspend fun remapAccount(fromAccountId: Long, toAccountId: Long?)
    fun getNeedsReviewCount(): Flow<Int>
    fun getNeedsReviewExpenses(): Flow<List<Expense>>
    suspend fun clearAllNeedsReview()
}
