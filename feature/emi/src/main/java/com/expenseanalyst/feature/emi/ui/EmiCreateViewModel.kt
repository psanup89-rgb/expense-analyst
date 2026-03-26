package com.expenseanalyst.feature.emi.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.usecase.CreateEmiFromExpenseUseCase
import com.expenseanalyst.domain.usecase.GetExpenseByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmiCreateUiState(
    val months: String = "12",
    val interestRate: String = "",
    val isCreating: Boolean = false,
    val createdGroupId: Long? = null,
    val error: String? = null,
    val expenseDescription: String = "",
    val expenseAmount: Double = 0.0,
    val expenseCurrency: String = "INR"
) {
    val parsedMonths: Int get() = months.toIntOrNull() ?: 0
    val parsedInterestRate: Double get() = interestRate.toDoubleOrNull() ?: 0.0
    val installmentPreview: Double
        get() {
            if (parsedMonths < 2 || expenseAmount <= 0) return 0.0
            val rate = parsedInterestRate
            return if (rate <= 0) {
                expenseAmount / parsedMonths
            } else {
                val r = rate / 12.0 / 100.0
                val n = parsedMonths.toDouble()
                expenseAmount * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1)
            }
        }
    val isValid: Boolean get() = parsedMonths in 2..60 && expenseAmount > 0
}

@HiltViewModel
class EmiCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val createEmiFromExpenseUseCase: CreateEmiFromExpenseUseCase
) : ViewModel() {

    private val expenseId: Long = checkNotNull(savedStateHandle["expenseId"])
    private val _ui = MutableStateFlow(EmiCreateUiState())
    val uiState = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val expense = getExpenseByIdUseCase(expenseId).first() ?: return@launch
            _ui.update {
                it.copy(
                    expenseDescription = expense.description,
                    expenseAmount = expense.amount,
                    expenseCurrency = expense.currencyCode
                )
            }
        }
    }

    fun onMonthsChange(value: String) = _ui.update { it.copy(months = value.filter { c -> c.isDigit() }) }
    fun onInterestRateChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { s ->
                val dotIdx = s.indexOf('.')
                if (dotIdx == -1) s else s.substring(0, dotIdx + 1) + s.substring(dotIdx + 1).filter { it.isDigit() }
            }
        _ui.update { it.copy(interestRate = filtered) }
    }

    fun createEmi(onDone: (Long) -> Unit) {
        val state = _ui.value
        if (!state.isValid) return
        viewModelScope.launch {
            _ui.update { it.copy(isCreating = true, error = null) }
            try {
                val expense = getExpenseByIdUseCase(expenseId).first() ?: return@launch
                val groupId = createEmiFromExpenseUseCase(
                    expense = expense,
                    numberOfMonths = state.parsedMonths,
                    annualInterestRate = state.parsedInterestRate
                )
                _ui.update { it.copy(isCreating = false, createdGroupId = groupId) }
                onDone(groupId)
            } catch (e: Exception) {
                _ui.update { it.copy(isCreating = false, error = e.message ?: "Failed to create EMI") }
            }
        }
    }
}
