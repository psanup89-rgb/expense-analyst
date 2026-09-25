package com.expenseanalyst.feature.analytics.ui

import com.expenseanalyst.domain.model.Expense

sealed class DrillDownFilter {
    data object Spent : DrillDownFilter()
    data object Income : DrillDownFilter()
    data class ByCategory(val categoryName: String) : DrillDownFilter()
    data class ByMerchant(val merchantName: String) : DrillDownFilter()

    /**
     * Transfers between the user's own accounts. A first-class filter rather than an extension
     * of [ByCategory]'s Split-Payments special case, because own-transfer rows keep their own
     * varied categories — the bucket is a classification, not a category name.
     */
    data object OwnTransfers : DrillDownFilter()
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val homeCurrencyCode: String = "SAR",
    val selectedMonthLabel: String = "",
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val prevMonthExpense: Double = 0.0,
    val avgDailySpend: Double = 0.0,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val dailySpend: List<DailySpend> = emptyList(),
    val topMerchants: List<MerchantSpend> = emptyList(),
    val canGoNext: Boolean = false,
    val drillDownTitle: String? = null,
    val drillDownExpenses: List<Expense> = emptyList()
)

data class CategorySpend(
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val amount: Double,
    val percentage: Float,
    /**
     * True for the synthetic buckets ("Split Payments", "Own Transfers") whose [amount] is
     * deliberately NOT part of [AnalyticsUiState.totalExpense]'s denominator, so [percentage] is
     * meaningless here and the UI must not render it as a normal progress bar.
     */
    val isExcludedFromTotal: Boolean = false,
    /**
     * Where a tap on this bar drills to. Defaults to this bucket's own category name, which is
     * right for every real category; the synthetic buckets override it.
     */
    val drillDownFilter: DrillDownFilter = DrillDownFilter.ByCategory(categoryName)
)

data class DailySpend(
    val day: Int,
    val amount: Double
)

data class MerchantSpend(
    val name: String,
    val amount: Double,
    val count: Int
)
