package com.expenseanalyst.domain.util

/**
 * Detects whether a free-text search query is actually an amount lookup — an exact figure
 * (e.g. "4386") or a range ("4000 to 4500") — so [ExpenseListViewModel] can branch to amount
 * filtering instead of the default description/merchant/tag substring search.
 *
 * Deliberately a standalone pure-Kotlin utility, not inlined into the ViewModel, matching the
 * house convention (CurrencyConversion, CategoryInference, NeedsReviewEvaluator) of keeping
 * parsing/matching logic in `:domain/util` where it's independently testable.
 */
object AmountSearchParser {

    // "4000 to 4500" — checked before the exact pattern, since a bare number is also a valid
    // prefix of this shape and would otherwise never get the chance to match as a range.
    private val rangePattern = Regex("""^\s*([\d]+(?:\.\d+)?)\s+to\s+([\d]+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE)
    private val exactPattern = Regex("""^\s*([\d]+(?:\.\d+)?)\s*$""")

    sealed class Result {
        /** Exact match against the expense's original-currency amount, tolerant of Double rounding. */
        data class Exact(val amount: Double) : Result()

        /** Inclusive range, already normalised so [min] <= [max] regardless of input order. */
        data class Range(val min: Double, val max: Double) : Result()

        /** Not a number/range — callers should fall back to the ordinary text search. */
        data object NotAnAmount : Result()
    }

    fun parse(query: String): Result {
        rangePattern.find(query)?.let { match ->
            val a = match.groupValues[1].toDoubleOrNull()
            val b = match.groupValues[2].toDoubleOrNull()
            if (a != null && b != null) return Result.Range(minOf(a, b), maxOf(a, b))
        }
        exactPattern.find(query)?.let { match ->
            match.groupValues[1].toDoubleOrNull()?.let { return Result.Exact(it) }
        }
        return Result.NotAnAmount
    }
}
