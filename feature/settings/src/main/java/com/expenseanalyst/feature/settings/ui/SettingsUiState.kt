package com.expenseanalyst.feature.settings.ui

data class SettingsUiState(
    val homeCurrencyCode: String = "SAR",
    val notificationCaptureEnabled: Boolean = true,
    val isCurrencyPickerVisible: Boolean = false,
    val currencySearchQuery: String = "",
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
