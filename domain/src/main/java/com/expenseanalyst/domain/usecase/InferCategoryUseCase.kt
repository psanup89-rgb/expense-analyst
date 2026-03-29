package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.repository.MerchantSearchRepository
import com.expenseanalyst.domain.util.CategoryInference
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 3-tier category inference for a single merchant.
 *
 * Tier 1 — User MerchantRules (instant, highest priority)
 * Tier 2 — Keyword matching via CategoryInference (instant)
 * Tier 3 — Google Places API (async, ~1s; only runs when enabled in Settings)
 *
 * Returns [InferenceResult] or null if all tiers fail.
 */
class InferCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val merchantSearchRepository: MerchantSearchRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) {
    suspend operator fun invoke(
        merchant: String,
        bankName: String? = null,
        smsBody: String? = null
    ): InferenceResult? {
        if (merchant.isBlank()) return null

        val categories = categoryRepository.getCategories().first()
        val merchantRules = merchantRuleRepository.getRules().first()

        // Tier 1: User-defined MerchantRule — rules-only pass (no bank keyword noise)
        val tier1 = CategoryInference.infer(
            merchant = merchant,
            bankName = null,
            categories = categories,
            smsBody = null,
            merchantRules = merchantRules
        )
        if (tier1 != null) {
            println("InferCategory [$merchant] Tier1 hit → ${tier1.name}")
            return InferenceResult(tier1, InferenceSource.MERCHANT_RULE)
        }

        // Tier 2: Keyword inference on merchant + bankName + SMS body
        val tier2 = CategoryInference.infer(
            merchant = merchant,
            bankName = bankName,
            categories = categories,
            smsBody = smsBody,
            merchantRules = emptyList()
        )
        if (tier2 != null) {
            println("InferCategory [$merchant] Tier2 hit → ${tier2.name}")
            return InferenceResult(tier2, InferenceSource.KEYWORD)
        }

        // Tier 3: Google Places API (only when enabled in Settings)
        val isPlacesEnabled = appPreferencesRepository.isGooglePlacesEnabled().first()
        println("InferCategory [$merchant] Tier3 — placesEnabled=$isPlacesEnabled")
        if (!isPlacesEnabled) return null
        val categoryName = merchantSearchRepository.searchMerchantCategory(merchant)
            ?: return null
        val matched = categories.find { it.name.equals(categoryName, ignoreCase = true) }
            ?: return null
        return InferenceResult(matched, InferenceSource.WEB_SEARCH)
    }
}

data class InferenceResult(
    val category: Category,
    val source: InferenceSource
)

enum class InferenceSource { MERCHANT_RULE, KEYWORD, WEB_SEARCH }
