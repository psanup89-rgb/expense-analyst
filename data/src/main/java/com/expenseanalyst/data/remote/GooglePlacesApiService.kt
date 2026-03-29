package com.expenseanalyst.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the Google Places API (New) Text Search endpoint to retrieve place types.
 * Uses `X-Goog-FieldMask: places.types` — cheapest projection, Basic tier ~$0.017/call.
 *
 * Parses the response via raw JsonElement API (no @Serializable classes needed).
 * Returns null on any network or parsing failure.
 */
@Singleton
class GooglePlacesApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Returns the list of Google place types for [merchantName], or null if the
     * request fails or no results are found.
     *
     * @param apiKey  Google Cloud API key with Places API (New) enabled.
     */
    suspend fun findPlaceTypes(merchantName: String, apiKey: String): List<String>? = runCatching {
        Log.d(TAG, "Searching Places for: $merchantName")

        // Escape any quotes in the merchant name for the JSON body
        val safeName = merchantName.replace("\\", "\\\\").replace("\"", "\\\"")

        val responseText: String = httpClient.post(
            "https://places.googleapis.com/v1/places:searchText"
        ) {
            header("X-Goog-Api-Key", apiKey)
            header("X-Goog-FieldMask", "places.types")
            contentType(ContentType.Application.Json)
            setBody("""{"textQuery":"$safeName"}""")
        }.bodyAsText()

        val root = lenientJson.parseToJsonElement(responseText).jsonObject
        Log.d(TAG, "Places raw=${responseText.take(300)}")

        // New API returns {"places":[{"types":[...]}]} — no status field; empty object = no results
        val types = root["places"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("types")?.jsonArray
            ?.map { it.jsonPrimitive.content }

        Log.d(TAG, "Place types: $types")
        types
    }.onFailure { e ->
        Log.e(TAG, "Places API call failed for '$merchantName'", e)
    }.getOrNull()

    private companion object {
        const val TAG = "GooglePlacesApi"
        val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
