package com.expenseanalyst.data.local.dao

import androidx.room.*
import com.expenseanalyst.data.local.entity.ExpenseEntity
import com.expenseanalyst.data.local.relation.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // ── With-Category (used by repository) ──────────────────────────────────

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_deleted = 0 ORDER BY date_utc_millis DESC")
    fun getAllExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_deleted = 0 AND date_utc_millis BETWEEN :startMillis AND :endMillis ORDER BY date_utc_millis DESC")
    fun getExpensesByDateRangeWithCategory(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_deleted = 0 AND category_id = :categoryId ORDER BY date_utc_millis DESC")
    fun getExpensesByCategoryWithCategory(categoryId: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseByIdWithCategory(id: Long): Flow<ExpenseWithCategory?>

    @Transaction
    @Query("SELECT * FROM expenses WHERE emi_group_id = :emiGroupId AND is_deleted = 0 ORDER BY emi_installment_number ASC")
    fun getExpensesByEmiGroupWithCategory(emiGroupId: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_deleted = 0 ORDER BY date_utc_millis DESC")
    suspend fun getActiveExpensesWithCategorySnapshot(): List<ExpenseWithCategory>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY date_utc_millis DESC")
    suspend fun getAllExpensesWithCategorySnapshot(): List<ExpenseWithCategory>

    // ── Raw entity queries (internal repo use) ───────────────────────────────

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseEntityById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("UPDATE expenses SET is_deleted = 1, updated_at_utc_millis = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("UPDATE expenses SET is_deleted = 0, updated_at_utc_millis = :updatedAt WHERE id = :id")
    suspend fun restore(id: Long, updatedAt: Long)

    @Query("UPDATE expenses SET is_deleted = 1, updated_at_utc_millis = :updatedAt WHERE emi_group_id = :emiGroupId AND date_utc_millis > :afterMillis AND is_deleted = 0")
    suspend fun softDeleteFutureEmiInstallments(emiGroupId: Long, afterMillis: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM expenses WHERE emi_group_id = :emiGroupId AND is_deleted = 0 AND date_utc_millis <= :nowMillis")
    suspend fun countPaidInstallments(emiGroupId: Long, nowMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_deleted = 0 AND bill_id = :billId ORDER BY date_utc_millis DESC")
    fun getExpensesByBillId(billId: Long): Flow<List<ExpenseWithCategory>>

    @Query("SELECT COUNT(*) FROM expenses WHERE account_id = :accountId AND is_deleted = 0")
    suspend fun countByAccount(accountId: Long): Int

    @Transaction
    @Query("SELECT * FROM expenses WHERE account_id = :accountId AND is_deleted = 0 ORDER BY date_utc_millis DESC")
    suspend fun getExpensesByAccount(accountId: Long): List<ExpenseWithCategory>

    @Query("UPDATE expenses SET account_id = :toAccountId WHERE account_id = :fromAccountId AND is_deleted = 0")
    suspend fun remapAccount(fromAccountId: Long, toAccountId: Long?)

    @Transaction
    @Query("""
        SELECT * FROM expenses
        WHERE is_deleted = 0
          AND transaction_type = 'INCOME'
          AND date_utc_millis >= :startMillis
          AND date_utc_millis < :endMillis
        ORDER BY amount DESC
    """)
    fun getIncomeByDateRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>
}
