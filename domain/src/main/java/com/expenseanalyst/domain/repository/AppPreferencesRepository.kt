package com.expenseanalyst.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    fun isNotificationCaptureEnabled(): Flow<Boolean>
    suspend fun setNotificationCaptureEnabled(enabled: Boolean)
}
