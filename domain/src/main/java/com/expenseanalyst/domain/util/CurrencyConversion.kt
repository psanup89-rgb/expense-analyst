package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.model.Expense
import kotlin.math.abs

data class ResolvedCurrencyConversion(
    val homeAmount: Double?,
    val exchangeRate: Double?
)

object CurrencyConversion {
    private const val TOLERANCE = 0.005

    fun resolve(
        expense: Expense,
        homeCurrencyCode: String,
        ratesByCode: Map<String, CurrencyRate>
    ): ResolvedCurrencyConversion {
        if (expense.currencyCode == homeCurrencyCode) {
            return ResolvedCurrencyConversion(
                homeAmount = expense.amount,
                exchangeRate = 1.0
            )
        }

        val homeRate = ratesByCode[homeCurrencyCode]
        val sourceRate = ratesByCode[expense.currencyCode]
        val derivedRate = if (
            homeRate != null &&
            sourceRate != null &&
            sourceRate.rateToBase > 0.0
        ) {
            homeRate.rateToBase / sourceRate.rateToBase
        } else {
            null
        }

        return if (derivedRate != null) {
            ResolvedCurrencyConversion(
                homeAmount = expense.amount * derivedRate,
                exchangeRate = derivedRate
            )
        } else {
            ResolvedCurrencyConversion(
                homeAmount = expense.homeAmount,
                exchangeRate = expense.exchangeRate
            )
        }
    }

    /**
     * Converts [amount] between two currencies through the shared base rate, or null when either
     * rate is missing. Same derivation as [resolve], for values that aren't an Expense row.
     */
    fun convert(
        amount: Double,
        fromCode: String,
        toCode: String,
        ratesByCode: Map<String, CurrencyRate>
    ): Double? {
        if (fromCode == toCode) return amount
        val toRate = ratesByCode[toCode]?.rateToBase ?: return null
        val fromRate = ratesByCode[fromCode]?.rateToBase?.takeIf { it > 0.0 } ?: return null
        return amount * toRate / fromRate
    }

    fun needsSync(
        expense: Expense,
        homeCurrencyCode: String,
        ratesByCode: Map<String, CurrencyRate>
    ): Boolean {
        val resolved = resolve(expense, homeCurrencyCode, ratesByCode)
        return differs(expense.homeAmount, resolved.homeAmount) ||
            differs(expense.exchangeRate, resolved.exchangeRate)
    }

    private fun differs(current: Double?, target: Double?): Boolean {
        return when {
            current == null && target == null -> false
            current == null || target == null -> true
            else -> abs(current - target) > TOLERANCE
        }
    }
}
