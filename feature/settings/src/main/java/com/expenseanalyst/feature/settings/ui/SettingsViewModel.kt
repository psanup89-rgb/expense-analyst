package com.expenseanalyst.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.ThemeMode
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.util.CurrencyConversion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val expenseRepository: ExpenseRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val formState = MutableStateFlow(SettingsUiState())

    val uiState = combine(
        combine(
            currencyRepository.getHomeCurrency(),
            appPreferencesRepository.isNotificationCaptureEnabled(),
            appPreferencesRepository.getThemeMode(),
            formState
        ) { homeCurrency, notificationEnabled, themeMode, state ->
            state.copy(
                homeCurrencyCode = homeCurrency,
                notificationCaptureEnabled = notificationEnabled,
                themeMode = themeMode
            )
        },
        appPreferencesRepository.isGooglePlacesEnabled(),
        appPreferencesRepository.getGooglePlacesApiKey()
    ) { state, googleEnabled, googleKey ->
        state.copy(
            googlePlacesEnabled = googleEnabled,
            googlePlacesApiKey = googleKey
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun showCurrencyPicker() = formState.update { it.copy(isCurrencyPickerVisible = true) }

    fun dismissCurrencyPicker() = formState.update {
        it.copy(isCurrencyPickerVisible = false, currencySearchQuery = "")
    }

    fun onCurrencySearchQueryChange(value: String) = formState.update {
        it.copy(currencySearchQuery = value)
    }

    fun clearMessage() = formState.update {
        it.copy(statusMessage = null, errorMessage = null)
    }

    fun toggleNotificationCapture(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setNotificationCaptureEnabled(enabled)
        }
    }

    fun toggleGooglePlaces(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setGooglePlacesEnabled(enabled)
        }
    }

    fun onGooglePlacesApiKeyChange(key: String) {
        viewModelScope.launch {
            appPreferencesRepository.setGooglePlacesApiKey(key)
        }
    }

    fun toggleApiKeyVisibility() = formState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(mode)
        }
    }

    fun updateHomeCurrency(currencyCode: String) {
        val normalizedCode = currencyCode.uppercase(Locale.US)
        if (normalizedCode == uiState.value.homeCurrencyCode && !uiState.value.isCurrencyPickerVisible) {
            return
        }

        viewModelScope.launch {
            formState.update {
                it.copy(
                    isSaving = true,
                    isCurrencyPickerVisible = false,
                    currencySearchQuery = "",
                    statusMessage = null,
                    errorMessage = null
                )
            }

            runCatching {
                if (currencyRepository.isStale()) {
                    currencyRepository.refreshRates()
                }

                val ratesByCode = currencyRepository.getRates()
                    .first()
                    .associateBy { it.currencyCode }

                val homeRate = ratesByCode[normalizedCode]
                val expenses = expenseRepository.getExpensesSnapshot(includeDeleted = true)

                expenses.forEach { expense ->
                    val resolved = CurrencyConversion.resolve(
                        expense = expense,
                        homeCurrencyCode = normalizedCode,
                        ratesByCode = ratesByCode
                    )
                    expenseRepository.updateExpense(
                        expense.copy(
                            homeAmount = resolved.homeAmount,
                            exchangeRate = resolved.exchangeRate
                        )
                    )
                }

                currencyRepository.setHomeCurrency(normalizedCode)
            }.onSuccess {
                formState.update {
                    it.copy(
                        isSaving = false,
                        statusMessage = "Home currency updated to $normalizedCode"
                    )
                }
            }.onFailure {
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Could not update home currency"
                    )
                }
            }
        }
    }
}
