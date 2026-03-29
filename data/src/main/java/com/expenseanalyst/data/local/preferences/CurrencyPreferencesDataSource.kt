package com.expenseanalyst.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyPreferencesDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) }
    )

    private val defaultHomeCurrency: String = "SAR"

    fun getHomeCurrency(): Flow<String> =
        dataStore.data.map { preferences ->
            preferences[HOME_CURRENCY_KEY] ?: defaultHomeCurrency
        }

    suspend fun setHomeCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[HOME_CURRENCY_KEY] = currencyCode.uppercase(Locale.US)
        }
    }

    fun isOnboardingCompleted(): Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_KEY] ?: false }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[ONBOARDING_KEY] = true }
    }

    fun isNotificationCaptureEnabled(): Flow<Boolean> =
        dataStore.data.map { it[NOTIFICATION_CAPTURE_KEY] ?: true }

    suspend fun setNotificationCaptureEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATION_CAPTURE_KEY] = enabled }
    }

    fun getThemeMode(): Flow<String> =
        dataStore.data.map { it[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE_KEY] = mode }
    }

    private companion object {
        const val PREFERENCES_FILE_NAME = "expense_analyst_preferences.preferences_pb"
        const val DEFAULT_THEME_MODE = "SYSTEM"
        val HOME_CURRENCY_KEY = stringPreferencesKey("home_currency_code")
        val ONBOARDING_KEY = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATION_CAPTURE_KEY = booleanPreferencesKey("notification_capture_enabled")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
