package com.expenseanalyst.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    // ExchangeRate-API free endpoint — no key required
    private val baseUrl = "https://open.er-api.com/v6/latest/USD"

    suspend fun fetchRates(): Map<String, Double>? = runCatching {
        val response: ExchangeRateResponse = httpClient.get(baseUrl).body()
        if (response.result == "success") response.rates else null
    }.getOrNull()
}

@Serializable
data class ExchangeRateResponse(
    val result: String,
    val rates: Map<String, Double> = emptyMap(),
    @SerialName("time_last_update_utc") val timeLastUpdate: String = ""
)
