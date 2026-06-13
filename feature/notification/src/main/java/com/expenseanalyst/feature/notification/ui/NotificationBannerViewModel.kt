package com.expenseanalyst.feature.notification.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import com.expenseanalyst.feature.notification.service.AutoSavedEvent
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

    val lastAutoSaved: StateFlow<AutoSavedEvent?> = combine(
        pendingManager.lastAutoSaved,
        appPreferencesRepository.isNotificationCaptureEnabled()
    ) { event, enabled ->
        if (enabled) event else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun consume() = pendingManager.consume()

    fun dismiss() = pendingManager.dismiss()
}
