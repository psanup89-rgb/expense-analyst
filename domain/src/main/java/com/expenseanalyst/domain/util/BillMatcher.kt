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
 * Tolerance is `max(AMOUNT_TOLERANCE_RATIO * totalDue, ABSOLUTE_TOLERANCE)` so small
 * bills aren't disqualified by a few cents of rounding while big bills still get
 * a percentage band.
 */
object BillMatcher {

    /** ±5% of totalDue is treated as a high-confidence match. */
    private const val AMOUNT_TOLERANCE_RATIO = 0.05
    /** Floor in home-currency units so small-bill rounding still matches. */
    private const val ABSOLUTE_TOLERANCE = 1.0

    fun findMatchingOpenBill(
        payment: Double,
        merchant: String?,
        openBills: List<Bill>
    ): Bill? {
        if (merchant.isNullOrBlank()) return null
        val merchantTrimmed = merchant.trim()
        return openBills.firstOrNull { bill -> isMatch(payment, merchantTrimmed, bill) }
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
