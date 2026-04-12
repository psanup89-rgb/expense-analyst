package com.expenseanalyst.data.repository

import android.util.Log
import com.expenseanalyst.data.BuildConfig
import com.expenseanalyst.data.remote.ClaudeApiService
import com.expenseanalyst.domain.repository.MerchantSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantSearchRepositoryImpl @Inject constructor(
    private val claudeApiService: ClaudeApiService
) : MerchantSearchRepository {

    override suspend fun searchMerchantCategory(merchantName: String): String? {
        if (BuildConfig.CLAUDE_API_KEY.isBlank() || BuildConfig.CLAUDE_API_BASE_URL.isBlank()) {
            Log.w(TAG, "Claude API key or base URL is blank — skipping Tier 3")
            return null
        }
        val category = claudeApiService.classifyMerchant(merchantName)
        Log.d(TAG, "Claude classified '$merchantName' → $category")
        return category
    }

    private companion object {
        const val TAG = "MerchantSearchRepo"
    }
}
