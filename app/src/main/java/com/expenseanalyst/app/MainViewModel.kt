package com.expenseanalyst.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.OnboardingRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import com.expenseanalyst.feature.notification.parser.TransactionDirection
import com.expenseanalyst.feature.notification.service.PendingNotificationManager
import com.expenseanalyst.feature.notification.service.TransactionAlertNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    onboardingRepository: OnboardingRepository,
    private val pendingManager: PendingNotificationManager,
    pendingNotificationRepository: PendingNotificationRepository
) : ViewModel() {

    /**
     * null = still loading, true = onboarding done, false = needs onboarding
     */
    val pendingInboxCount: StateFlow<Int> = pendingNotificationRepository.getCount()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = 0)

    val isOnboardingCompleted: StateFlow<Boolean?> = onboardingRepository
        .isOnboardingCompleted()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Route to navigate to when the app is opened from a system notification.
     * Set by MainActivity when an OPEN_ADD_EXPENSE intent is received.
     */
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    fun setPendingRoute(route: String) {
        _pendingRoute.value = route
    }

    fun consumePendingRoute() {
        _pendingRoute.value = null
    }

    /** Clears the in-app notification banner after an expense is successfully saved. */
    fun dismissBanner() {
        pendingManager.dismiss()
    }

    /**
     * Fires a fake SAR 150.00 transaction through the full notification pipeline:
     * - System tray notification (to verify POST_NOTIFICATIONS + channel setup)
     * - In-app banner (to verify PendingNotificationManager → NotificationBanner flow)
     *
     * Use the "Test Notification" button in Settings to trigger this.
     */
    fun testNotification(context: Context) {
        val fake = ParsedTransaction(
            amount = 150.00,
            currencyCode = "SAR",
            type = TransactionDirection.DEBIT,
            merchant = "Test Merchant",
            accountLast4 = "1234",
            referenceNumber = "TEST001",
            bankName = "Test Bank"
        )
        pendingManager.enqueue(fake)
        TransactionAlertNotification.post(context.applicationContext, fake)
    }
}
