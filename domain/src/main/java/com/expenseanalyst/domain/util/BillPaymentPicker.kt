package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType
import kotlin.math.abs

/**
 * Pairs bills and the expenses that paid them by hand (owner's choice, Nov 2026: suggest, never
 * link automatically). A utility bill paid by card (Saudi Electricity) is an ordinary EXPENSE whose
 * payee name rarely matches the bill's biller, so nothing links it at capture.
 */
object BillPaymentPicker {

    const val WINDOW_DAYS = 60L
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val TOLERANCE = 0.01

    private fun sameAmount(amount: Double, total: Double?) = total != null && abs(amount - total) < TOLERANCE

    /** Open bills of exactly this expense's amount and currency — the hint by "Link to Bill". */
    fun sameAmountOpenBills(expense: Expense, bills: List<Bill>): List<Bill> =
        if (expense.billId != null || expense.isDeleted ||
            expense.transactionType !in setOf(TransactionType.EXPENSE, TransactionType.PAYMENT)
        ) {
            emptyList()
        } else {
            bills.filter {
                !it.isDeleted && it.status != BillStatus.SETTLED &&
                    it.currencyCode == expense.currencyCode && sameAmount(expense.amount, it.totalDue)
            }
        }

    /**
     * Expenses "Mark as paid" offers for [bill]: expenses and payments in the bill's currency, not
     * on any bill, from the last [WINDOW_DAYS] days — exact-amount matches first, then newest.
     */
    fun candidates(bill: Bill, expenses: List<Expense>, nowMillis: Long): List<Expense> {
        val since = nowMillis - WINDOW_DAYS * DAY_MS
        return expenses
            .filter {
                !it.isDeleted && it.billId == null && it.currencyCode == bill.currencyCode &&
                    it.transactionType in setOf(TransactionType.EXPENSE, TransactionType.PAYMENT) &&
                    it.date.toEpochMilliseconds() >= since
            }
            .sortedWith(
                compareByDescending<Expense> { sameAmount(it.amount, bill.totalDue) }
                    .thenByDescending { it.date.toEpochMilliseconds() }
            )
    }
}
