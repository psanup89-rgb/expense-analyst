package com.expenseanalyst.feature.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingInboxViewModel @Inject constructor(
    private val repository: PendingNotificationRepository,
    private val billRepository: BillRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(PendingInboxUiState())

    val uiState = combine(repository.getAll(), _ui) { items, ui ->
        ui.copy(items = items, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PendingInboxUiState()
    )

    fun requestDismiss(id: Long) = _ui.update { it.copy(pendingDismissId = id) }
    fun cancelDismiss() = _ui.update { it.copy(pendingDismissId = null) }
    fun confirmDismiss() {
        val id = _ui.value.pendingDismissId ?: return
        _ui.update { it.copy(pendingDismissId = null) }
        viewModelScope.launch { repository.delete(id) }
    }

    fun requestDismissAll() = _ui.update { it.copy(showDismissAllConfirm = true) }
    fun cancelDismissAll() = _ui.update { it.copy(showDismissAllConfirm = false) }
    fun confirmDismissAll() {
        _ui.update { it.copy(showDismissAllConfirm = false) }
        viewModelScope.launch { repository.deleteAll() }
    }

    // ── Bill actions ──────────────────────────────────────────────────────────

    fun requestSaveBill(id: Long) = _ui.update { it.copy(pendingSaveBillId = id) }
    fun cancelSaveBill() = _ui.update { it.copy(pendingSaveBillId = null) }
    fun confirmSaveBill() {
        val id = _ui.value.pendingSaveBillId ?: return
        _ui.update { it.copy(pendingSaveBillId = null) }
        viewModelScope.launch {
            val item = repository.getById(id) ?: return@launch
            val homeCurrency = currencyRepository.getHomeCurrency().first()
            billRepository.saveBill(
                Bill(
                    billerName = item.billerName ?: item.merchantName ?: "Unknown",
                    accountId = null,
                    totalDue = if (item.amount > 0) item.amount else null,
                    minimumDue = null,
                    currencyCode = homeCurrency,
                    dueDateMillis = item.dueDateMillis,
                    statementPeriodStart = null,
                    statementPeriodEnd = null,
                    status = BillStatus.PENDING,
                    sourceType = SourceType.SMS_AUTO,
                    createdAtMillis = System.currentTimeMillis(),
                    isDeleted = false,
                    reference = null
                )
            )
            repository.delete(id)
        }
    }

    fun requestUpdateBill(id: Long) = _ui.update { it.copy(pendingUpdateBillId = id) }
    fun cancelUpdateBill() = _ui.update { it.copy(pendingUpdateBillId = null) }
    fun confirmUpdateBill() {
        val id = _ui.value.pendingUpdateBillId ?: return
        _ui.update { it.copy(pendingUpdateBillId = null) }
        viewModelScope.launch {
            val item = repository.getById(id) ?: return@launch
            val billId = item.linkedBillId ?: return@launch
            val existing = billRepository.getBillById(billId).first() ?: return@launch
            billRepository.updateBill(
                existing.copy(
                    totalDue = if (item.amount > 0) item.amount else existing.totalDue,
                    dueDateMillis = item.dueDateMillis ?: existing.dueDateMillis
                )
            )
            repository.delete(id)
        }
    }
}
