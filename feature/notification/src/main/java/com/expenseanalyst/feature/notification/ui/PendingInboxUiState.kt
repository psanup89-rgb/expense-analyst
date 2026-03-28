package com.expenseanalyst.feature.notification.ui

import com.expenseanalyst.domain.model.PendingNotification

data class PendingInboxUiState(
    val items: List<PendingNotification> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDismissId: Long? = null,
    val showDismissAllConfirm: Boolean = false
)
