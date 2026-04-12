package com.expenseanalyst.data.remote

import android.util.Log
import com.expenseanalyst.data.BuildConfig
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
 * Calls the Claude Messages API (or a compatible proxy) to classify a merchant name
 * into one of the app's expense categories.
 *
 * Base URL and API key are read from BuildConfig, which in turn reads them from
 * local.properties:
 *   CLAUDE_API_KEY=<key>
 *   CLAUDE_API_BASE_URL=https://your-proxy.example.com   (defaults to api.anthropic.com)
 *
 * Uses `claude-haiku-3-5` — cheapest and fastest model, sufficient for single-label
 * classification. max_tokens=20 keeps cost minimal (category names are short).
 *
 * Returns null on network failure, invalid response, or unrecognised category text.
 */
@Singleton
class ClaudeApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun classifyMerchant(merchantName: String): String? = runCatching {
        Log.d(TAG, "Classifying merchant via Claude: $merchantName")

        val safeName = merchantName.replace("\\", "\\\\").replace("\"", "\\\"")

        val responseText: String = httpClient.post(
            "${BuildConfig.CLAUDE_API_BASE_URL}/v1/messages"
        ) {
            header("Authorization", "Bearer ${BuildConfig.CLAUDE_API_KEY}")
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "model": "claude-haiku-4.5",
                  "max_tokens": 20,
                  "messages": [{
                    "role": "user",
                    "content": "Classify this merchant into ONE expense category. Choose from: Food, Transport, Shopping, Bills, Entertainment, Health, Education, Groceries, Other. Reply with ONLY the category name, nothing else. Merchant: \"$safeName\""
                  }]
                }
                """.trimIndent()
            )
        }.bodyAsText()

        Log.d(TAG, "Claude raw response: ${responseText.take(200)}")

        // Response shape: {"content":[{"type":"text","text":"Food"}],...}
        val root = lenientJson.parseToJsonElement(responseText).jsonObject
        val category = root["content"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?.trim()

        // Guard against hallucinations — only accept known category names
        category?.takeIf { it in VALID_CATEGORIES }.also {
            Log.d(TAG, "Claude category for '$merchantName': $it (raw=$category)")
        }
    }.onFailure { e ->
        Log.e(TAG, "Claude API call failed for '$merchantName'", e)
    }.getOrNull()

    private companion object {
        const val TAG = "ClaudeApiService"
        val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }
        val VALID_CATEGORIES = setOf(
            "Food", "Transport", "Shopping", "Bills",
            "Entertainment", "Health", "Education", "Groceries", "Other"
        )
    }
}
