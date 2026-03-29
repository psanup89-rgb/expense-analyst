package com.expenseanalyst.feature.settings.ui

import com.expenseanalyst.domain.model.ThemeMode

data class SettingsUiState(
    val homeCurrencyCode: String = "SAR",
    val notificationCaptureEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isCurrencyPickerVisible: Boolean = false,
    val currencySearchQuery: String = "",
    val isSaving: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
