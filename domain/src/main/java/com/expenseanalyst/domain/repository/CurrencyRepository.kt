package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.CurrencyRate
import kotlinx.coroutines.flow.Flow

interface CurrencyRepository {
    fun getRates(): Flow<List<CurrencyRate>>
    fun getRate(currencyCode: String): Flow<CurrencyRate?>
    fun getHomeCurrency(): Flow<String>
    suspend fun setHomeCurrency(currencyCode: String)
    suspend fun refreshRates()
    suspend fun isStale(): Boolean
}
