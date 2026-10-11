package com.expenseanalyst.domain.util

import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SameTransactionKeyTest {

    private val riyadh = TimeZone.of("Asia/Riyadh")

    // 2026-10-07 00:07:21 UTC = 03:07:21 in Riyadh
    private val t = 1_791_331_641_000L

    @Test
    fun `two messages about one transfer seconds apart share a key`() {
        assertEquals(
            SameTransactionKey.of(10_000.0, "SOME PERSON", t, riyadh),
            SameTransactionKey.of(10_000.0, " some person ", t + 13_000, riyadh)
        )
    }

    @Test
    fun `a different amount, merchant or day is a different transaction`() {
        val key = SameTransactionKey.of(4_000.0, "Recipient", t, riyadh)
        assertNotEquals(key, SameTransactionKey.of(4_000.01, "Recipient", t, riyadh))
        assertNotEquals(key, SameTransactionKey.of(4_000.0, "Someone Else", t, riyadh))
        assertNotEquals(key, SameTransactionKey.of(4_000.0, "Recipient", t + 86_400_000L, riyadh))
    }

    @Test
    fun `the day is the local calendar day, not the UTC one`() {
        // 22:30 and 23:30 UTC on 6 Oct are 01:30 and 02:30 on 7 Oct in Riyadh — the same local day,
        // but 20:30 UTC (23:30 local, 6 Oct) is not.
        val base = 1_791_318_600_000L // 2026-10-06 20:30 UTC
        assertNotEquals(
            SameTransactionKey.of(1.0, "X", base, riyadh),
            SameTransactionKey.of(1.0, "X", base + 2 * 3_600_000L, riyadh)
        )
        assertEquals(
            SameTransactionKey.of(1.0, "X", base + 2 * 3_600_000L, riyadh),
            SameTransactionKey.of(1.0, "X", base + 3 * 3_600_000L, riyadh)
        )
    }

    @Test
    fun `no merchant, no key`() {
        assertNull(SameTransactionKey.of(1.0, null, t, riyadh))
        assertNull(SameTransactionKey.of(1.0, "  ", t, riyadh))
    }
}
