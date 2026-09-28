package com.expenseanalyst.feature.notification.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.feature.notification.service.TransactionAlertNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val repository: PendingNotificationRepository,
    private val billRepository: BillRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(PendingInboxUiState())

    val uiState = combine(repository.getAll(), _ui) { items, ui ->
        ui.copy(items = items.filter { it.pendingType == "BILL" || it.pendingType == "BILL_REMINDER" }, isLoading = false)
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
        viewModelScope.launch {
            repository.delete(id)
            TransactionAlertNotification.cancelForBill(context, id)
        }
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
            val now = System.currentTimeMillis()
            billRepository.saveBill(
                Bill(
                    billerName = item.billerName ?: item.merchantName ?: "Unknown",
                    accountId = null,
                    totalDue = if (item.amount > 0) item.amount else null,
                    minimumDue = null,
                    // The statement's own currency — an Airtel bill is INR whatever the home
                    // currency is. (This used the home currency before.)
                    currencyCode = item.currencyCode,
                    dueDateMillis = item.dueDateMillis,
                    statementPeriodStart = null,
                    statementPeriodEnd = null,
                    status = BillStatus.PENDING,
                    sourceType = SourceType.SMS_AUTO,
                    createdAtMillis = now,
                    isDeleted = false,
                    reference = null,
                    // reminders that arrived while the statement waited here
                    reminderCount = item.reminderCount,
                    lastReminderAtMillis = if (item.reminderCount > 0) now else null
                )
            )
            repository.delete(id)
            TransactionAlertNotification.cancelForBill(context, id)
        }
    }

    /** A BILL_REMINDER card: count its reminders on the saved bill, then clear the card. */
    fun linkReminder(id: Long) {
        viewModelScope.launch {
            val item = repository.getById(id) ?: return@launch
            val billId = item.linkedBillId ?: return@launch
            billRepository.addReminders(billId, item.reminderCount.coerceAtLeast(1), System.currentTimeMillis())
            repository.delete(id)
            TransactionAlertNotification.cancelForBill(context, id)
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
            if (item.reminderCount > 0) billRepository.addReminders(billId, item.reminderCount, System.currentTimeMillis())
            repository.delete(id)
            TransactionAlertNotification.cancelForBill(context, id)
        }
    }
}
