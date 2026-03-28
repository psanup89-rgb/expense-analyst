package com.expenseanalyst.feature.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingInboxViewModel @Inject constructor(
    private val repository: PendingNotificationRepository
) : ViewModel() {

    val uiState = repository.getAll()
        .map { items -> PendingInboxUiState(items = items, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PendingInboxUiState()
        )

    fun dismiss(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun dismissAll() {
        viewModelScope.launch { repository.deleteAll() }
    }
}
