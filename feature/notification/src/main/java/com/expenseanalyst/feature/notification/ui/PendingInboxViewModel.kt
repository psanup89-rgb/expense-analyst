package com.expenseanalyst.feature.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingInboxViewModel @Inject constructor(
    private val repository: PendingNotificationRepository
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
}
