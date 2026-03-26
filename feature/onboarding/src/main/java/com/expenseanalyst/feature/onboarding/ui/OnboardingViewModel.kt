package com.expenseanalyst.feature.onboarding.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.core.util.CurrencyCatalog
import com.expenseanalyst.domain.usecase.CompleteOnboardingUseCase
import com.expenseanalyst.domain.usecase.SetHomeCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,  // 0=welcome, 1=currency, 2=notifications, 3=sms-import
    val selectedCurrencyCode: String = "",
    val currencySearchQuery: String = "",
    val isCompleting: Boolean = false
) {
    val allCurrencies get() = CurrencyCatalog.all
    val filteredCurrencies get() = CurrencyCatalog.all.filter { currency ->
        val q = currencySearchQuery.trim()
        q.isBlank() ||
            currency.code.contains(q, ignoreCase = true) ||
            currency.displayName.contains(q, ignoreCase = true)
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val setHomeCurrencyUseCase: SetHomeCurrencyUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun nextStep() = _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(3)) }
    fun prevStep() = _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }

    fun selectCurrency(code: String) = _uiState.update {
        if (it.selectedCurrencyCode == code) it.copy(selectedCurrencyCode = "")
        else it.copy(selectedCurrencyCode = code, currencySearchQuery = "")
    }

    fun onCurrencySearchChange(query: String) = _uiState.update { it.copy(currencySearchQuery = query) }

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun completeOnboarding(onDone: (smsAutoStart: String?) -> Unit, smsAutoStart: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }
            setHomeCurrencyUseCase(_uiState.value.selectedCurrencyCode)
            completeOnboardingUseCase()
            _uiState.update { it.copy(isCompleting = false) }
            onDone(smsAutoStart)
        }
    }
}
