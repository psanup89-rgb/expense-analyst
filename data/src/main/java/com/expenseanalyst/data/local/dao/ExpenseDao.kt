package com.expenseanalyst.data.local.dao

import androidx.room.*
import com.expenseanalyst.data.local.entity.ExpenseEntity
import com.expenseanalyst.data.local.relation.ExpenseWithCategory
import com.expenseanalyst.domain.util.NeedsReviewEvaluator
import com.expenseanalyst.domain.util.ReviewReason
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
    suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long>

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

    /**
     * Needs-review scope. Beyond the persisted `needs_review` flag this also picks up any
     * unclassified TRANSFER, because those rows count toward neither Spent nor Received and are
     * therefore unresolved by definition — including rows captured before classification existed,
     * which have no persisted reason and were deliberately not backfilled.
     *
     * This is NOT the forbidden "recompute review reasons at display time". That rule exists
     * because a reason like MISSING_MERCHANT stops being detectable once the blank merchant is
     * backfilled with the bank name. `transaction_type = 'TRANSFER' AND transfer_classification
     * IS NULL` is a structural fact about stored columns that stays accurate forever.
     *
     * Kept in SQL, and identically in both queries, so the list, its header count and the
     * bottom-nav badge cannot disagree.
     */
    @Query(
        """
        SELECT COUNT(*) FROM expenses
        WHERE is_deleted = 0
          AND (needs_review = 1 OR (transaction_type = 'TRANSFER' AND transfer_classification IS NULL AND loan_id IS NULL))
        """
    )
    fun getNeedsReviewCount(): Flow<Int>

    @Transaction
    @Query(
        """
        SELECT * FROM expenses
        WHERE is_deleted = 0
          AND (needs_review = 1 OR (transaction_type = 'TRANSFER' AND transfer_classification IS NULL AND loan_id IS NULL))
        ORDER BY date_utc_millis DESC
        """
    )
    fun getNeedsReviewExpenses(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_deleted = 0 AND loan_id = :loanId ORDER BY date_utc_millis ASC")
    fun getExpensesByLoanId(loanId: Long): Flow<List<ExpenseWithCategory>>

    /**
     * Links (or, with a null [loanId], unlinks) one expense to a loan. Targeted single-column
     * write for the same reason as [updateDescription]: a full-row updateExpense would null
     * account_number via the entity mapper and rewrite the tag join table.
     */
    @Query(
        """
        UPDATE expenses SET loan_id = :loanId, updated_at_utc_millis = :updatedAt
        WHERE id = :id AND is_deleted = 0
        """
    )
    suspend fun setLoanId(id: Long, loanId: Long?, updatedAt: Long): Int

    /**
     * Links a row as a loan leg and stamps the direction in one write, so the two can never be
     * seen half-applied. [classification] is written only for a TRANSFER (an outbound leg is
     * EXTERNAL, an inbound one EXTERNAL_IN) — that is how SpendClassifier.isLoanRepayment knows
     * which way money moved on a row whose type can't say. Other row types keep their own
     * direction (INCOME is inherently inbound). Also drops UNCLASSIFIED_TRANSFER, since linking
     * resolves it.
     */
    @Transaction
    suspend fun linkLoanLeg(id: Long, loanId: Long, classification: String, updatedAt: Long): Int {
        val existing = getExpenseEntityById(id) ?: return 0
        if (existing.isDeleted) return 0
        val remaining = NeedsReviewEvaluator.remove(existing.needsReviewReasons, ReviewReason.UNCLASSIFIED_TRANSFER)
        val isTransfer = existing.transactionType == "TRANSFER"
        return applyLoanLeg(
            id = id,
            loanId = loanId,
            classification = if (isTransfer) classification else existing.transferClassification,
            reasons = NeedsReviewEvaluator.encode(remaining).takeIf { it.isNotBlank() },
            needsReview = remaining.isNotEmpty(),
            updatedAt = updatedAt
        )
    }

    @Query(
        """
        UPDATE expenses
        SET loan_id = :loanId, transfer_classification = :classification,
            needs_review_reasons = :reasons, needs_review = :needsReview, updated_at_utc_millis = :updatedAt
        WHERE id = :id AND is_deleted = 0
        """
    )
    suspend fun applyLoanLeg(
        id: Long,
        loanId: Long,
        classification: String?,
        reasons: String?,
        needsReview: Boolean,
        updatedAt: Long
    ): Int

    @Query("UPDATE expenses SET needs_review = 0, updated_at_utc_millis = :updatedAt WHERE needs_review = 1 AND is_deleted = 0")
    suspend fun clearAllNeedsReview(updatedAt: Long)

    /**
     * Targeted single-column update used by the notification inline-reply ("Add note") path.
     * Deliberately avoids a full-row round-trip: the entity mapper would null out
     * account_number, and a read-modify-write would race an open Edit Expense screen.
     * Returns rows affected — 0 means the expense is missing or soft-deleted.
     */
    @Query(
        """
        UPDATE expenses SET description = :description, updated_at_utc_millis = :updatedAt
        WHERE id = :id AND is_deleted = 0
        """
    )
    suspend fun updateDescription(id: Long, description: String, updatedAt: Long): Int

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

    @Transaction
    @Query("SELECT * FROM expenses WHERE is_reimbursable = 1 AND is_deleted = 0 ORDER BY date_utc_millis DESC")
    fun getReimbursableExpensesWithCategory(): Flow<List<ExpenseWithCategory>>

    /**
     * Targeted single-column update for the Reimbursements screen's "mark reimbursed" /
     * "undo" toggle. Pass null to move an item back to pending. Guarded on is_reimbursable = 1
     * so a stale UI action can never flip the flag on an expense that was un-marked meanwhile.
     * Returns rows affected — 0 means the expense is missing, soft-deleted, or no longer
     * reimbursable.
     */
    @Query(
        """
        UPDATE expenses SET reimbursed_date_millis = :reimbursedDateMillis, updated_at_utc_millis = :updatedAt
        WHERE id = :id AND is_deleted = 0 AND is_reimbursable = 1
        """
    )
    suspend fun updateReimbursedDate(id: Long, reimbursedDateMillis: Long?, updatedAt: Long): Int

    /**
     * Reclassifies an already-recorded expense as a BNPL split payment: category + type only.
     * Targeted update for the same reason as [updateDescription] — a full-row updateExpense
     * would null account_number (via the entity mapper) and rewrite the tag join table.
     * `transaction_type` is written as the raw enum name to match ExpenseMapper's convention.
     */
    @Query(
        """
        UPDATE expenses SET category_id = :categoryId, transaction_type = 'PAYMENT', updated_at_utc_millis = :updatedAt
        WHERE id = :id AND is_deleted = 0
        """
    )
    suspend fun reclassifyAsSplitPayment(id: Long, categoryId: Long, updatedAt: Long): Int

    @Query(
        """
        UPDATE expenses
        SET transfer_classification = :classification,
            needs_review_reasons    = :reasons,
            needs_review            = :needsReview,
            updated_at_utc_millis   = :updatedAt
        WHERE id = :id AND is_deleted = 0 AND transaction_type = 'TRANSFER'
        """
    )
    suspend fun applyTransferClassification(
        id: Long,
        classification: String,
        reasons: String?,
        needsReview: Boolean,
        updatedAt: Long
    ): Int

    /**
     * Records whether a transfer went to the user's own account or to someone else, and clears
     * the resulting review flag.
     *
     * Targeted rather than a full-row updateExpense for the same reason as [updateDescription] —
     * that path nulls account_number via the entity mapper and rewrites the tag join table.
     * Guarded on `transaction_type = 'TRANSFER'`, mirroring [updateReimbursedDate]'s
     * `is_reimbursable = 1` guard, so a stale UI action can never stamp a classification onto a
     * row that is no longer a transfer.
     *
     * Note on the reason recompute: review reasons are computed once at capture time and
     * persisted, and must never be recomputed at *display* time (a blank merchant is backfilled
     * with the bank name before persisting, so it can't be detected after the fact). Classifying
     * is an explicit user mutation, not a display-time recompute, so recomputing here is correct
     * — and doing it in a ViewModel's uiState flow would not be. Only UNCLASSIFIED_TRANSFER is
     * dropped; needs_review clears only if no other reason survives, since a transfer can also
     * be flagged for a missing merchant or an unresolved account.
     *
     * Returns rows affected — 0 means the expense is missing, soft-deleted, or not a transfer.
     */
    @Transaction
    suspend fun classifyTransfer(id: Long, classification: String, updatedAt: Long): Int {
        val existing = getExpenseEntityById(id) ?: return 0
        val remaining = NeedsReviewEvaluator.remove(
            existing.needsReviewReasons,
            ReviewReason.UNCLASSIFIED_TRANSFER
        )
        return applyTransferClassification(
            id = id,
            classification = classification,
            reasons = NeedsReviewEvaluator.encode(remaining).takeIf { it.isNotBlank() },
            needsReview = remaining.isNotEmpty(),
            updatedAt = updatedAt
        )
    }
}
