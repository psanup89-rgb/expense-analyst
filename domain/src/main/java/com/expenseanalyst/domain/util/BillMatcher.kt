package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Bill
import kotlin.math.abs
import kotlin.math.max

/**
 * Strict bill auto-linker. Used when a PAYMENT-type transaction needs to be linked
 * to an open bill without involving the user. The previous behaviour matched by
 * billerName substring alone, which routinely linked May payments to last month's
 * bill with a completely different amount (GitHub issue #11).
 *
 * A match requires BOTH:
 *  - billerName substring match (case-insensitive, either direction)
 *  - amount match: payment is within tolerance of totalDue, OR the bill has a
 *    minimumDue and the payment covers it
 *
 * Fallback (Nov 2026): a payment of exactly one open bill's total, in that bill's currency, links
 * to it even when the payee name differs — see findMatchingOpenBill.
 *
 * Tolerance is `max(AMOUNT_TOLERANCE_RATIO * totalDue, ABSOLUTE_TOLERANCE)` so small
 * bills aren't disqualified by a few cents of rounding while big bills still get
 * a percentage band.
 */
object BillMatcher {

    /** ±5% of totalDue is treated as a high-confidence match. */
    private const val AMOUNT_TOLERANCE_RATIO = 0.05
    /** Floor in home-currency units so small-bill rounding still matches. */
    private const val ABSOLUTE_TOLERANCE = 1.0
    private const val EXACT_TOLERANCE = 0.01

    fun findMatchingOpenBill(
        payment: Double,
        merchant: String?,
        openBills: List<Bill>,
        /** When given, the exact-amount fallback only considers bills in this currency. */
        currencyCode: String? = null
    ): Bill? {
        val merchantTrimmed = merchant?.trim()?.takeIf { it.isNotBlank() }
        merchantTrimmed?.let { m -> openBills.firstOrNull { bill -> isMatch(payment, m, bill) } }?.let { return it }
        // Payee names rarely name the biller the way its statement does ("IDFC Bank Credit Card
        // Bill" / "BillPayments" vs "IDFC FIRST Bank"), so a payment of EXACTLY one open bill's
        // total in its currency is that bill. Unique only — two bills for the same amount, no guess.
        if (currencyCode == null) return null
        return openBills.filter {
            it.currencyCode == currencyCode && it.totalDue != null && abs(payment - it.totalDue) < EXACT_TOLERANCE
        }.singleOrNull()
    }

    private fun isMatch(payment: Double, merchant: String, bill: Bill): Boolean {
        val biller = bill.billerName.trim()
        val nameMatches = biller.isNotBlank() && (
            biller.contains(merchant, ignoreCase = true) ||
                merchant.contains(biller, ignoreCase = true)
            )
        if (!nameMatches) return false

        val totalDue = bill.totalDue
        if (totalDue != null) {
            val tolerance = max(totalDue * AMOUNT_TOLERANCE_RATIO, ABSOLUTE_TOLERANCE)
            if (abs(payment - totalDue) <= tolerance) return true
        }
        val minDue = bill.minimumDue
        if (minDue != null && payment + ABSOLUTE_TOLERANCE >= minDue) return true

        // No reliable amount signal — refuse to link blindly. Issue #11.
        return false
    }
}
