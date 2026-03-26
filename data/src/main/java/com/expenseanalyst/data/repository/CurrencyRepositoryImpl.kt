package com.expenseanalyst.data.repository

import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.data.local.dao.CurrencyRateDao
import com.expenseanalyst.data.local.entity.CurrencyRateEntity
import com.expenseanalyst.data.local.preferences.CurrencyPreferencesDataSource
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.data.remote.CurrencyApiService
import com.expenseanalyst.data.remote.SeedCurrencyRates
import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val currencyRateDao: CurrencyRateDao,
    private val preferencesDataSource: CurrencyPreferencesDataSource,
    private val apiService: CurrencyApiService
) : CurrencyRepository {

    override fun getRates(): Flow<List<CurrencyRate>> =
        currencyRateDao.getAllRates().map { rates -> rates.map { it.toDomain() } }

    override fun getRate(currencyCode: String): Flow<CurrencyRate?> =
        currencyRateDao.getRate(currencyCode.uppercase(Locale.US)).map { it?.toDomain() }

    override fun getHomeCurrency(): Flow<String> = preferencesDataSource.getHomeCurrency()

    override suspend fun setHomeCurrency(currencyCode: String) {
        preferencesDataSource.setHomeCurrency(currencyCode)
    }

    override suspend fun refreshRates() {
        val now = DateTimeUtil.nowMillis()
        // Try live API first; fall back to seed rates if offline or error
        val ratesMap = apiService.fetchRates() ?: SeedCurrencyRates.usdBaseRates
        val entities = ratesMap.map { (code, rate) ->
            CurrencyRateEntity(
                currencyCode = code.uppercase(Locale.US),
                rateToBase = rate,
                lastUpdatedUtcMillis = now
            )
        }
        currencyRateDao.insertAll(entities)
    }

    override suspend fun isStale(): Boolean {
        val lastUpdated = currencyRateDao.getLastUpdated() ?: return true
        val ageMs = DateTimeUtil.nowMillis() - lastUpdated
        return ageMs >= ONE_DAY_MS
    }

    private companion object {
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    }
}
