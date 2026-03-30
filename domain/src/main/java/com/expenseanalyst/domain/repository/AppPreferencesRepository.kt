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
}
