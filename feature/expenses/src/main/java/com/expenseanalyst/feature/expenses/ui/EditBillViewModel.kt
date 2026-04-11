package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.repository.BillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditBillViewModel @Inject constructor(
    private val billRepository: BillRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: Long = checkNotNull(savedStateHandle["billId"])

    private val _uiState = MutableStateFlow(EditBillUiState())
    val uiState: StateFlow<EditBillUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bill = billRepository.getBillById(billId).first()
            if (bill != null) {
                _uiState.update {
                    it.copy(
                        billerName = bill.billerName,
                        totalDue = bill.totalDue?.let { v -> "%.2f".format(v) } ?: "",
                        minimumDue = bill.minimumDue?.let { v -> "%.2f".format(v) } ?: "",
                        currencyCode = bill.currencyCode,
                        dueDateMillis = bill.dueDateMillis,
                        status = bill.status,
                        reference = bill.reference ?: "",
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Bill not found") }
            }
        }
    }

    fun onBillerNameChange(value: String) = _uiState.update { it.copy(billerName = value) }
    fun onTotalDueChange(value: String) = _uiState.update { it.copy(totalDue = value) }
    fun onMinimumDueChange(value: String) = _uiState.update { it.copy(minimumDue = value) }
    fun onCurrencyChange(value: String) = _uiState.update { it.copy(currencyCode = value.uppercase().take(3)) }
    fun onDueDateChange(millis: Long?) = _uiState.update { it.copy(dueDateMillis = millis) }
    fun onStatusChange(status: BillStatus) = _uiState.update { it.copy(status = status) }
    fun onReferenceChange(value: String) = _uiState.update { it.copy(reference = value) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.billerName.isBlank()) {
            _uiState.update { it.copy(error = "Biller name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val existing = billRepository.getBillById(billId).first() ?: return@launch
            billRepository.updateBill(
                existing.copy(
                    billerName = state.billerName.trim(),
                    totalDue = state.totalDue.toDoubleOrNull(),
                    minimumDue = state.minimumDue.toDoubleOrNull(),
                    currencyCode = state.currencyCode.ifBlank { existing.currencyCode },
                    dueDateMillis = state.dueDateMillis,
                    status = state.status,
                    reference = state.reference.trim().takeIf { it.isNotBlank() }
                )
            )
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            onSaved()
        }
    }
}
