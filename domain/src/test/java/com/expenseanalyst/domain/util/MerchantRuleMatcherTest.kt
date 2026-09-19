package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.MerchantRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MerchantRuleMatcherTest {

    private val rules = listOf(
        MerchantRule(id = 1, merchantPattern = "noon", categoryId = 10, categoryName = "Shopping", createdAt = 0),
        MerchantRule(id = 2, merchantPattern = "starbucks", categoryId = 20, categoryName = "Food & Drinks", createdAt = 0)
    )

    @Test
    fun `matches merchant containing the rule pattern`() {
        val result = MerchantRuleMatcher.findMatch("NOON.COM UAE", rules)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `match is case-insensitive`() {
        val result = MerchantRuleMatcher.findMatch("Starbucks Coffee", rules)
        assertEquals(2L, result?.id)
    }

    @Test
    fun `no match returns null`() {
        assertNull(MerchantRuleMatcher.findMatch("Amazon", rules))
    }

    @Test
    fun `null or blank merchant returns null`() {
        assertNull(MerchantRuleMatcher.findMatch(null, rules))
        assertNull(MerchantRuleMatcher.findMatch("", rules))
    }

    @Test
    fun `empty rules list returns null`() {
        assertNull(MerchantRuleMatcher.findMatch("noon", emptyList()))
    }
}
