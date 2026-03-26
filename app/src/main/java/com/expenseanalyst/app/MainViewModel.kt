package com.expenseanalyst.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    onboardingRepository: OnboardingRepository
) : ViewModel() {

    /**
     * null = still loading, true = onboarding done, false = needs onboarding
     */
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
}
