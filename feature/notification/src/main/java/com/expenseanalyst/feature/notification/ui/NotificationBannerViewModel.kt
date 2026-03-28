package com.expenseanalyst.feature.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import com.expenseanalyst.feature.notification.service.PendingNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotificationBannerViewModel @Inject constructor(
    private val pendingManager: PendingNotificationManager,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    val pending: StateFlow<ParsedTransaction?> = combine(
        pendingManager.pending,
        appPreferencesRepository.isNotificationCaptureEnabled()
    ) { parsed, enabled ->
        if (enabled) parsed else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val lastPendingId: StateFlow<Long?> = pendingManager.lastPendingId

    fun consume(): ParsedTransaction? = pendingManager.consume()

    fun dismiss() = pendingManager.dismiss()
}
