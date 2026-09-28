package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Expense
import kotlin.math.abs

/**
 * Suggests which pending reimbursable expenses an incoming payment repays. Only a suggestion:
 * the owner chose to be asked ("Possible reimbursement" in Review) rather than auto-link.
 *
 * A payment matches either one pending expense of the same amount and currency (the most recent
 * wins on ties), or — an employer repaying a whole claim at once — ALL pending expenses in that
 * currency whose sum equals it. Arbitrary subsets are not searched: with a handful of pending
 * items that finds coincidences, not claims.
 */
object ReimbursementMatcher {

    private const val TOLERANCE = 0.01
    const val WINDOW_DAYS = 180L
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Reimbursable, not yet repaid or linked, not deleted. */
    fun isPending(e: Expense): Boolean =
        e.isReimbursable && e.reimbursedDate == null && e.reimbursedById == null && !e.isDeleted

    fun suggest(
        amount: Double,
        currencyCode: String,
        paymentDateMillis: Long,
        expenses: List<Expense>
    ): List<Expense>? {
        val candidates = expenses.filter {
            isPending(it) && it.currencyCode == currencyCode &&
                it.date.toEpochMilliseconds() <= paymentDateMillis + DAY_MS &&
                it.date.toEpochMilliseconds() >= paymentDateMillis - WINDOW_DAYS * DAY_MS
        }
        if (candidates.isEmpty()) return null
        candidates.filter { abs(it.amount - amount) < TOLERANCE }
            .maxByOrNull { it.date.toEpochMilliseconds() }
            ?.let { return listOf(it) }
        if (candidates.size > 1 && abs(candidates.sumOf { it.amount } - amount) < TOLERANCE) return candidates
        return null
    }
}
