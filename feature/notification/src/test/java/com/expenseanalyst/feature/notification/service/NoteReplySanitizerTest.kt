package com.expenseanalyst.feature.notification.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NoteReplySanitizerTest {

    @Test
    fun `returns null for input with no usable text`() {
        assertNull(NoteReplySanitizer.sanitize(null))
        assertNull(NoteReplySanitizer.sanitize(""))
        assertNull(NoteReplySanitizer.sanitize("   "))
        assertNull(NoteReplySanitizer.sanitize("\n\t "))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals("hello", NoteReplySanitizer.sanitize("  hello  "))
    }

    @Test
    fun `collapses internal whitespace to single spaces`() {
        assertEquals("a b", NoteReplySanitizer.sanitize("a\nb"))
        assertEquals("a b", NoteReplySanitizer.sanitize("a   b"))
        assertEquals("Lunch with Sam", NoteReplySanitizer.sanitize("Lunch \t with\n\nSam"))
    }

    @Test
    fun `leaves text at the cap unchanged`() {
        val exact = "x".repeat(NoteReplySanitizer.MAX_LENGTH)
        assertEquals(exact, NoteReplySanitizer.sanitize(exact))
    }

    @Test
    fun `caps overlong text`() {
        val result = NoteReplySanitizer.sanitize("x".repeat(300))
        assertEquals(NoteReplySanitizer.MAX_LENGTH, result?.length)
    }

    @Test
    fun `preserves non-latin text`() {
        // Saudi bank SMS are routinely Arabic; the note may be too.
        assertEquals("غداء مع سام", NoteReplySanitizer.sanitize("  غداء مع سام  "))
    }

    @Test
    fun `preserves emoji and never truncates mid surrogate pair`() {
        assertEquals("coffee ☕️🥐", NoteReplySanitizer.sanitize("coffee ☕️🥐"))

        // Place a surrogate pair so the cap would otherwise split it.
        val padded = "x".repeat(NoteReplySanitizer.MAX_LENGTH - 1) + "🥐" + "tail"
        val result = NoteReplySanitizer.sanitize(padded)!!
        assertEquals(NoteReplySanitizer.MAX_LENGTH - 1, result.length)
        assertEquals(false, result.last().isHighSurrogate())
    }
}
