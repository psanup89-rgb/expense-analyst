package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.util.BnplInstallmentMatcher.Charge
import com.expenseanalyst.domain.util.BnplInstallmentMatcher.Link
import com.expenseanalyst.domain.util.BnplInstallmentMatcher.Purchase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Cases mirror the real Jul–Sep 2026 Tabby history this matcher was written from. */
class BnplInstallmentMatcherTest {

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_780_000_000_000L

    @Test
    fun `instalment count is inferred from the amounts`() {
        assertEquals(4, BnplInstallmentMatcher.instalmentCount(599.0, 149.75))
        assertEquals(3, BnplInstallmentMatcher.instalmentCount(1878.0, 626.0))
        assertEquals(3, BnplInstallmentMatcher.instalmentCount(1288.0, 429.33))
        assertEquals(3, BnplInstallmentMatcher.instalmentCount(781.0, 260.34))  // rounding split
        assertNull(BnplInstallmentMatcher.instalmentCount(599.0, 150.0))       // not an even split
        assertNull(BnplInstallmentMatcher.instalmentCount(599.0, 599.0))       // N = 1 isn't BNPL
    }

    @Test
    fun `a charge arriving seconds before the confirmation still links as 1 of N`() {
        val links = BnplInstallmentMatcher.match(
            listOf(Purchase(1, "Tabby", "Centrepoint", 599.0, t0 + 6_000)),
            listOf(Charge(10, "Tabby", 149.75, t0))
        )
        assertEquals(listOf(Link(10, 1, 1, 4)), links)
    }

    @Test
    fun `monthly charges are numbered in date order`() {
        val links = BnplInstallmentMatcher.match(
            listOf(Purchase(1, "Tabby", "IKEA", 1878.0, t0)),
            listOf(
                Charge(12, "Tabby", 626.0, t0 + 61 * day),
                Charge(10, "Tabby", 626.0, t0),
                Charge(11, "Tabby", 626.0, t0 + 30 * day)
            )
        )
        assertEquals(listOf(Link(10, 1, 1, 3), Link(11, 1, 2, 3), Link(12, 1, 3, 3)), links)
    }

    @Test
    fun `a purchase never takes more charges than its count`() {
        val links = BnplInstallmentMatcher.match(
            listOf(Purchase(1, "Tabby", "IKEA", 1878.0, t0)),
            (0..3).map { Charge(10L + it, "Tabby", 626.0, t0 + it * 30 * day) }
        )
        assertEquals(3, links.size)
        assertTrue(links.none { it.chargeId == 13L })
    }

    @Test
    fun `charges from the other provider are never linked`() {
        val links = BnplInstallmentMatcher.match(
            listOf(Purchase(1, "Tabby", "IKEA", 1878.0, t0)),
            listOf(Charge(10, "Tamara", 626.0, t0))
        )
        assertTrue(links.isEmpty())
    }

    @Test
    fun `a charge long after the window is left alone`() {
        val links = BnplInstallmentMatcher.match(
            listOf(Purchase(1, "Tabby", "IKEA", 1878.0, t0)),
            listOf(Charge(10, "Tabby", 626.0, t0 + 200 * day))
        )
        assertTrue(links.isEmpty())
    }

    @Test
    fun `two purchases with different splits each get their own charges`() {
        val links = BnplInstallmentMatcher.match(
            listOf(
                Purchase(1, "Tabby", "Magrabi", 1288.0, t0),
                Purchase(2, "Tabby", "Centrepoint", 599.0, t0 + 16 * day)
            ),
            listOf(
                Charge(10, "Tabby", 429.33, t0),
                Charge(11, "Tabby", 149.75, t0 + 16 * day)
            )
        )
        assertEquals(setOf(Link(10, 1, 1, 3), Link(11, 2, 1, 4)), links.toSet())
    }

    @Test
    fun `existing links are kept and new charges continue the numbering`() {
        val links = BnplInstallmentMatcher.match(
            listOf(Purchase(1, "Tabby", "IKEA", 1878.0, t0)),
            listOf(
                Charge(10, "Tabby", 626.0, t0, linkedPurchaseId = 1),
                Charge(11, "Tabby", 626.0, t0 + 30 * day)
            )
        )
        assertEquals(listOf(Link(10, 1, 1, 3), Link(11, 1, 2, 3)), links)
    }

    @Test
    fun `a stated instalment count must agree with the charge`() {
        // Tamara: "Split in 3" for 1,585.75 → 528.58 per instalment
        val stated = Purchase(1, "Tamara", "Ikea Store", 1585.75, t0, count = 3)
        assertEquals(listOf(Link(10, 1, 1, 3)), BnplInstallmentMatcher.match(listOf(stated), listOf(Charge(10, "Tamara", 528.58, t0))))
        // 1,585.75 / 5 = 317.15 divides evenly, but the confirmation said 3 — not this purchase
        assertTrue(BnplInstallmentMatcher.match(listOf(stated), listOf(Charge(11, "Tamara", 317.15, t0))).isEmpty())
    }

    @Test
    fun `a Tamara charge combining several due instalments stays unlinked`() {
        val links = BnplInstallmentMatcher.match(
            listOf(Purchase(1, "Tamara", "Ikea Store", 1585.75, t0, count = 3)),
            listOf(Charge(10, "Tamara", 528.58 + 799.33, t0 + 30 * day))
        )
        assertTrue(links.isEmpty())
    }
}
