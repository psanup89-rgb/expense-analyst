package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.LentItem

/**
 * Where a loan stands, in the loan's own currency.
 *
 * [principal] is the loan's recorded amount, not the sum of its outgoing legs: lending can predate
 * the app's history (see [LentItem]). [repaid] is every linked repayment converted into the loan's
 * currency. Repaying more than was lent is ignored (owner's choice, Oct 2026): [remaining] stops at
 * zero and the extra is never counted as income — the repayment legs stay out of Received like any
 * other loan leg.
 */
data class LoanBalance(
    val currencyCode: String,
    val principal: Double,
    val repaid: Double
) {
    val remaining: Double = (principal - repaid).coerceAtLeast(0.0)

    /** Within a cent of the principal counts as repaid, so rounding in conversions can't block it. */
    val isFullyRepaid: Boolean = principal > 0.0 && repaid >= principal - TOLERANCE

    /** [remaining] in the home currency, so loans in different currencies can be added up. */
    fun remainingInHome(loan: LentItem, homeCurrencyCode: String, ratesByCode: Map<String, CurrencyRate>): Double {
        CurrencyConversion.convert(remaining, currencyCode, homeCurrencyCode, ratesByCode)?.let { return it }
        val loanHome = loan.homeAmount
        if (loanHome != null && principal > 0.0) return remaining * loanHome / principal
        return remaining
    }

    companion object {
        const val TOLERANCE = 0.01

        /**
         * [legs] may include rows of other loans and outgoing legs; only this loan's repayments are
         * summed. A repayment in another currency is converted at today's rates, falling back to
         * the ratio of the loan's own home amount when a rate is missing, and to its face value
         * when neither is known.
         */
        fun of(loan: LentItem, legs: List<Expense>, ratesByCode: Map<String, CurrencyRate>): LoanBalance {
            val repaid = legs
                .filter { it.loanId == loan.id && SpendClassifier.isLoanRepayment(it) }
                .sumOf { toLoanCurrency(it, loan, ratesByCode) }
            return LoanBalance(loan.currencyCode, loan.amount, repaid)
        }

        private fun toLoanCurrency(leg: Expense, loan: LentItem, ratesByCode: Map<String, CurrencyRate>): Double {
            if (leg.currencyCode == loan.currencyCode) return leg.amount
            CurrencyConversion.convert(leg.amount, leg.currencyCode, loan.currencyCode, ratesByCode)?.let { return it }
            val loanHome = loan.homeAmount
            val legHome = leg.homeAmount
            if (loanHome != null && loanHome > 0.0 && legHome != null) return legHome * loan.amount / loanHome
            return leg.amount
        }
    }
}
