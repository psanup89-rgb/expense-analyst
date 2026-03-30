package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.preferences.CurrencyPreferencesDataSource
import com.expenseanalyst.domain.model.ThemeMode
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppPreferencesRepositoryImpl @Inject constructor(
    private val dataSource: CurrencyPreferencesDataSource
) : AppPreferencesRepository {
    override fun isNotificationCaptureEnabled(): Flow<Boolean> =
        dataSource.isNotificationCaptureEnabled()

    override suspend fun setNotificationCaptureEnabled(enabled: Boolean) =
        dataSource.setNotificationCaptureEnabled(enabled)

    override fun getThemeMode(): Flow<ThemeMode> =
        dataSource.getThemeMode().map { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
        }

    override suspend fun setThemeMode(mode: ThemeMode) =
        dataSource.setThemeMode(mode.name)

    override fun isGooglePlacesEnabled(): Flow<Boolean> =
        dataSource.isGooglePlacesEnabled()

    override suspend fun setGooglePlacesEnabled(enabled: Boolean) =
        dataSource.setGooglePlacesEnabled(enabled)
}
