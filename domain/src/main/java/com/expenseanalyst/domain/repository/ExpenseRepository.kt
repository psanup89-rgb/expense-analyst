package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransferClassification
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

    /** Targeted single-column update. Returns rows affected (0 if missing or soft-deleted). */
    suspend fun updateDescription(id: Long, description: String): Int

    /** All expenses flagged reimbursable, regardless of reimbursement status. */
    fun getReimbursableExpenses(): Flow<List<Expense>>

    /**
     * Marks (or, passing null, un-marks) an expense as reimbursed. Targeted update — does not
     * touch any other field. Returns rows affected (0 if missing, soft-deleted, or no longer
     * flagged reimbursable).
     */
    suspend fun markReimbursed(id: Long, reimbursedDate: Instant?): Int

    /**
     * Reclassifies an already-recorded expense as a BNPL split payment (category + type only,
     * targeted update). Returns rows affected (0 if missing or soft-deleted).
     */
    suspend fun reclassifyAsSplitPayment(id: Long, categoryId: Long): Int

    /**
     * Targeted update recording whether a TRANSFER went to the user's own account or to someone
     * else, clearing the UNCLASSIFIED_TRANSFER review reason. Returns rows affected — 0 means
     * the expense is missing, soft-deleted, or not a transfer.
     */
    suspend fun classifyTransfer(id: Long, classification: TransferClassification): Int

    /** Every non-deleted expense carrying [tagId], newest first. */
    fun getExpensesByTag(tagId: Long): Flow<List<Expense>>

    /** Every non-deleted row linked to [loanId], oldest first. */
    fun getExpensesByLoan(loanId: Long): Flow<List<Expense>>

    /**
     * Links an expense to a loan as its outgoing or incoming leg. Targeted update — not
     * updateExpense. For a TRANSFER it also stamps the direction (see ExpenseDao.linkLoanLeg).
     */
    suspend fun linkToLoan(id: Long, loanId: Long, isRepayment: Boolean): Int

    /** Removes an expense's loan link so it counts toward the totals again. */
    suspend fun unlinkFromLoan(id: Long): Int
}
