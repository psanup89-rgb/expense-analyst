package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.model.TransferRecipientRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TransferRecipientMatcherTest {

    private fun rule(key: String, classification: TransferClassification = TransferClassification.EXTERNAL) =
        TransferRecipientRule(
            id = 1,
            recipientKey = key,
            recipientDisplayName = key,
            classification = classification,
            createdAt = 0L
        )

    // ── normalize ──

    @Test
    fun `normalize trims upper-cases and collapses internal whitespace`() {
        assertEquals("SAMUEL RAJASEKAR", TransferRecipientMatcher.normalize("  samuel   rajasekar "))
    }

    @Test
    fun `normalize returns null for null or blank`() {
        assertNull(TransferRecipientMatcher.normalize(null))
        assertNull(TransferRecipientMatcher.normalize(""))
        assertNull(TransferRecipientMatcher.normalize("   "))
    }

    // ── findRule ──

    @Test
    fun `finds a rule regardless of case and spacing`() {
        val rules = listOf(rule("SAMUEL RAJASEKAR"))
        assertEquals(rules[0], TransferRecipientMatcher.findRule("samuel  rajasekar ", rules))
    }

    /**
     * The assertion that documents why MerchantRuleMatcher was not reused: it matches by
     * case-insensitive substring containment, which for person names would let one rule claim
     * unrelated people.
     */
    @Test
    fun `matching is exact, so a short rule never swallows a longer name`() {
        val rules = listOf(rule("RAJ"))
        assertNull(TransferRecipientMatcher.findRule("RAJASEKAR", rules))
        assertNull(TransferRecipientMatcher.findRule("RAJESH KUMAR", rules))
        assertNull(TransferRecipientMatcher.findRule("SIVARAJ", rules))
        assertEquals(rules[0], TransferRecipientMatcher.findRule("raj", rules))
    }

    @Test
    fun `returns null for a blank name or an empty rule list`() {
        assertNull(TransferRecipientMatcher.findRule(null, listOf(rule("SAMUEL RAJASEKAR"))))
        assertNull(TransferRecipientMatcher.findRule("  ", listOf(rule("SAMUEL RAJASEKAR"))))
        assertNull(TransferRecipientMatcher.findRule("SAMUEL RAJASEKAR", emptyList()))
    }

    @Test
    fun `carries the classification through`() {
        val rules = listOf(rule("ANOOP SASEEDHARAN", TransferClassification.OWN_ACCOUNT))
        assertEquals(
            TransferClassification.OWN_ACCOUNT,
            TransferRecipientMatcher.findRule("Anoop Saseedharan", rules)?.classification
        )
    }
}
