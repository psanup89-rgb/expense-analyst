package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.model.Expense

/**
 * A bill's status from ALL the payments linked to it, summed in the bill's own currency.
 *
 * The three places that set a status used to compare only the payment being linked against the
 * amount due, and in home currency: a bill paid in two parts stayed "Partial" after the second
 * payment, unlinking one of two payments marked it "Partial" even when the other covered it, and
 * an INR bill (Axis) was compared against a SAR figure.
 */
object BillSettlement {

    /** Rounding: a SAR 4,830 payment settles a SAR 4,829.82 statement; a few cents short does too. */
    private const val TOLERANCE = 0.5

    fun status(
        totalDue: Double?,
        billCurrency: String,
        payments: List<Expense>,
        homeCurrency: String,
        ratesByCode: Map<String, CurrencyRate>
    ): BillStatus {
        val live = payments.filterNot { it.isDeleted }
        if (live.isEmpty()) return BillStatus.PENDING
        if (totalDue == null) return BillStatus.SETTLED
        val paid = live.sumOf { amountInCurrency(it, billCurrency, homeCurrency, ratesByCode) ?: 0.0 }
        return if (paid >= totalDue - TOLERANCE) BillStatus.SETTLED else BillStatus.PARTIAL
    }

    /** [payment] in [currency]: as-is, through the rate table, or via the stored home amount. */
    fun amountInCurrency(
        payment: Expense,
        currency: String,
        homeCurrency: String,
        ratesByCode: Map<String, CurrencyRate>
    ): Double? {
        if (payment.currencyCode == currency) return payment.amount
        val from = ratesByCode[payment.currencyCode]?.rateToBase
        val to = ratesByCode[currency]?.rateToBase
        if (from != null && to != null && from > 0.0) return payment.amount * to / from
        return if (currency == homeCurrency) payment.homeAmount else null
    }

    private const val SAME_CYCLE_MILLIS = 7L * 24 * 60 * 60 * 1000

    /**
     * Whether a new statement is a correction of an open bill (same billing cycle) rather than
     * next cycle's bill: both due dates known and within a week. The inbox offers "Update Bill"
     * only then — it used to offer it for any open bill of the same biller, with no "Add" option,
     * so the September Al Rajhi statement could only overwrite the unpaid-looking August one.
     */
    fun isSameCycle(openBillDueMillis: Long?, statementDueMillis: Long?): Boolean =
        openBillDueMillis != null && statementDueMillis != null &&
            kotlin.math.abs(openBillDueMillis - statementDueMillis) <= SAME_CYCLE_MILLIS
}
