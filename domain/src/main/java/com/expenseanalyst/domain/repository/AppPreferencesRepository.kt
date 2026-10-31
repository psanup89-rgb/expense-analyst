package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    fun isNotificationCaptureEnabled(): Flow<Boolean>
    suspend fun setNotificationCaptureEnabled(enabled: Boolean)
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun isGooglePlacesEnabled(): Flow<Boolean>
    suspend fun setGooglePlacesEnabled(enabled: Boolean)
    /** Daily 12:00 bill reminders (BillReminderWorker). On by default. */
    fun isBillRemindersEnabled(): Flow<Boolean>
    suspend fun setBillRemindersEnabled(enabled: Boolean)
}
