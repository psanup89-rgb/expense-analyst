package com.expenseanalyst.feature.analytics.ui

import com.expenseanalyst.domain.model.Expense

sealed class DrillDownFilter {
    data object Spent : DrillDownFilter()
    data object Income : DrillDownFilter()
    data class ByCategory(val categoryName: String) : DrillDownFilter()
    data class ByMerchant(val merchantName: String) : DrillDownFilter()
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
    val percentage: Float
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
