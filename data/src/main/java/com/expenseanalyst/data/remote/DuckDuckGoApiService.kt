package com.expenseanalyst.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls DuckDuckGo Instant Answer API to look up a merchant's business type.
 * No API key required. Returns null on any network or parsing failure.
 */
@Singleton
class DuckDuckGoApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun searchMerchant(merchantName: String): DuckDuckGoResponse? = runCatching {
        val encoded = java.net.URLEncoder.encode(merchantName, "UTF-8")
        val response: DuckDuckGoResponse = httpClient.get(
            "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
        ).body()
        response
    }.getOrNull()
}

@Serializable
data class DuckDuckGoResponse(
    @SerialName("AbstractText") val abstractText: String = "",
    @SerialName("Entity") val entity: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("RelatedTopics") val relatedTopics: List<RelatedTopic> = emptyList()
)

@Serializable
data class RelatedTopic(
    @SerialName("Text") val text: String = ""
)
