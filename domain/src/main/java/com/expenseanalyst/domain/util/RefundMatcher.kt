package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType

/**
 * Matches a Refund-category INCOME transaction back to the original EXPENSE it refunds, so the
 * refund can inherit that expense's account and payment method instead of the guesses
 * [PendingNotificationManager]/`SmsImportViewModel` would otherwise make from the refund SMS's
 * own (often sparse) wording.
 *
 * Deliberately narrow, per the owner's explicit decisions: amount-only match (refund SMS rarely
 * carry a clean merchant name), most-recent-match on ties, and this is called only for
 * transactions [CategoryInference] already resolved to "Refund" via keyword matching — it never
 * expands *what* counts as a refund, only fills in account/payment method once one is already
 * identified as one.
 */
object RefundMatcher {

    private const val WINDOW_DAYS = 90L
    private const val WINDOW_MILLIS = WINDOW_DAYS * 24L * 60 * 60 * 1000

    /**
     * @param allExpenses a full snapshot (any transaction type) — used both as the candidate
     * pool and to find originals already claimed by an earlier refund via
     * [Expense.refundOriginalExpenseId], so the same purchase is never matched twice.
     */
    fun findMatch(
        refundAmount: Double,
        refundCurrencyCode: String,
        refundDateMillis: Long,
        allExpenses: List<Expense>
    ): Expense? {
        val alreadyClaimedIds = allExpenses.mapNotNull { it.refundOriginalExpenseId }.toSet()
        val windowStart = refundDateMillis - WINDOW_MILLIS
        return allExpenses
            .filter {
                !it.isDeleted &&
                    it.transactionType == TransactionType.EXPENSE &&
                    it.id !in alreadyClaimedIds &&
                    it.currencyCode == refundCurrencyCode &&
                    kotlin.math.abs(it.amount - refundAmount) < 0.005 &&
                    it.date.toEpochMilliseconds() in windowStart..refundDateMillis
            }
            .maxByOrNull { it.date.toEpochMilliseconds() }
    }
}
