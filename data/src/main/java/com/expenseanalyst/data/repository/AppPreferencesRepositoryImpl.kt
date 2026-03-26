package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.preferences.CurrencyPreferencesDataSource
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AppPreferencesRepositoryImpl @Inject constructor(
    private val dataSource: CurrencyPreferencesDataSource
) : AppPreferencesRepository {
    override fun isNotificationCaptureEnabled(): Flow<Boolean> =
        dataSource.isNotificationCaptureEnabled()

    override suspend fun setNotificationCaptureEnabled(enabled: Boolean) =
        dataSource.setNotificationCaptureEnabled(enabled)
}
