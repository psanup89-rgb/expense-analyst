package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.preferences.CurrencyPreferencesDataSource
import com.expenseanalyst.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val preferencesDataSource: CurrencyPreferencesDataSource
) : OnboardingRepository {
    override fun isOnboardingCompleted(): Flow<Boolean> =
        preferencesDataSource.isOnboardingCompleted()

    override suspend fun setOnboardingCompleted() =
        preferencesDataSource.setOnboardingCompleted()
}
