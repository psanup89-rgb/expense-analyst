package com.expenseanalyst.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TagNamesTest {

    @Test
    fun `normalize trims and collapses whitespace but keeps case`() {
        assertEquals("Villa Maintenance", TagNames.normalize("  Villa   Maintenance "))
    }

    @Test
    fun `normalize caps the length`() {
        assertEquals(TagNames.MAX_LENGTH, TagNames.normalize("x".repeat(100)).length)
    }

    @Test
    fun `blank names are invalid`() {
        assertFalse(TagNames.isValid(""))
        assertFalse(TagNames.isValid("   "))
        assertTrue(TagNames.isValid("Haven"))
    }

    /** The case that produced the real Subscription / subscriptions duplicate pair. */
    @Test
    fun `names match ignoring case and spacing`() {
        assertTrue(TagNames.sameName("Subscription", "subscription"))
        assertTrue(TagNames.sameName(" Tax  Deductible", "tax deductible"))
    }

    @Test
    fun `different names do not match, including plurals`() {
        assertFalse(TagNames.sameName("Subscription", "subscriptions"))
        assertFalse(TagNames.sameName("Haven", "Havens"))
    }
}
