package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType
import kotlin.math.abs

/**
 * One card-bill payment usually arrives as TWO messages — the paying bank's debit ("… IDFC Bank
 * Credit Card Bill" by UPI from savings) and the card's or bill-pay service's confirmation
 * ("BillPayments") — seconds to minutes apart, with different payee names, so neither dedup
 * catches them (Nov 2026: IDFC INR 7,313.64 and OneCard INR 299 each saved twice). Both are
 * PAYMENTs, so no total moves; the owner wants both on the same bill. They are linked together
 * (LinkBillPaymentUseCase) and counted ONCE wherever a bill's payments are added up.
 *
 * Twins: both PAYMENT, same currency, same amount (to the cent), within [WINDOW_MILLIS].
 * The cost of the rule is that two genuinely separate equal part-payments within two days count
 * once — rare, and it shows as "Partial" rather than wrongly "Settled".
 */
object BillPaymentTwins {

    const val WINDOW_MILLIS = 2L * 24 * 60 * 60 * 1000
    private const val AMOUNT_TOLERANCE = 0.01

    fun areTwins(a: Expense, b: Expense): Boolean =
        a.id != b.id &&
            a.transactionType == TransactionType.PAYMENT && b.transactionType == TransactionType.PAYMENT &&
            !a.isDeleted && !b.isDeleted &&
            a.currencyCode == b.currencyCode &&
            abs(a.amount - b.amount) < AMOUNT_TOLERANCE &&
            abs(a.date.toEpochMilliseconds() - b.date.toEpochMilliseconds()) <= WINDOW_MILLIS

    fun twinsOf(payment: Expense, candidates: List<Expense>): List<Expense> =
        candidates.filter { areTwins(payment, it) }

    /** [payments] with each twin pair (or chain) reduced to its earliest row. */
    fun distinct(payments: List<Expense>): List<Expense> {
        val kept = mutableListOf<Expense>()
        for (p in payments.sortedBy { it.date.toEpochMilliseconds() }) {
            if (kept.none { areTwins(it, p) }) kept += p
        }
        return kept
    }
}
