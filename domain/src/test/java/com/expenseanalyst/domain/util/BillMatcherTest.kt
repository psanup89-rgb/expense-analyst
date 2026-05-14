package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BillMatcherTest {

    private fun bill(
        id: Long,
        biller: String,
        totalDue: Double? = null,
        minimumDue: Double? = null,
        status: BillStatus = BillStatus.PENDING
    ) = Bill(
        id = id,
        billerName = biller,
        totalDue = totalDue,
        minimumDue = minimumDue,
        currencyCode = "INR",
        status = status,
        sourceType = SourceType.MANUAL,
        createdAtMillis = 0L
    )

    @Test
    fun `exact amount match returns the bill`() {
        val b = bill(1, "Axis Bank", totalDue = 1000.0)
        val match = BillMatcher.findMatchingOpenBill(1000.0, "Axis Bank", listOf(b))
        assertEquals(1L, match?.id)
    }

    @Test
    fun `payment within 5 percent tolerance matches`() {
        val b = bill(1, "Axis Bank", totalDue = 1000.0)
        val match = BillMatcher.findMatchingOpenBill(1040.0, "Axis Bank", listOf(b))
        assertEquals(1L, match?.id)
    }

    @Test
    fun `payment outside 5 percent tolerance does not match`() {
        val b = bill(1, "Axis Bank", totalDue = 1000.0)
        // 10% off — outside tolerance, no minimum due to fall back to
        val match = BillMatcher.findMatchingOpenBill(1100.0, "Axis Bank", listOf(b))
        assertNull(match)
    }

    @Test
    fun `partial payment covering minimum due matches`() {
        val b = bill(1, "Axis Bank", totalDue = 5000.0, minimumDue = 500.0)
        val match = BillMatcher.findMatchingOpenBill(500.0, "Axis Bank", listOf(b))
        assertEquals(1L, match?.id)
    }

    @Test
    fun `payment below minimum due does not match`() {
        val b = bill(1, "Axis Bank", totalDue = 5000.0, minimumDue = 500.0)
        val match = BillMatcher.findMatchingOpenBill(200.0, "Axis Bank", listOf(b))
        assertNull(match)
    }

    @Test
    fun `merchant mismatch does not match even when amount is right`() {
        val b = bill(1, "Axis Bank", totalDue = 1000.0)
        val match = BillMatcher.findMatchingOpenBill(1000.0, "HDFC Bank", listOf(b))
        assertNull(match)
    }

    @Test
    fun `null or blank merchant returns null`() {
        val b = bill(1, "Axis Bank", totalDue = 1000.0)
        assertNull(BillMatcher.findMatchingOpenBill(1000.0, null, listOf(b)))
        assertNull(BillMatcher.findMatchingOpenBill(1000.0, "   ", listOf(b)))
    }

    @Test
    fun `bill without amount info returns null even on merchant match`() {
        val b = bill(1, "Axis Bank", totalDue = null, minimumDue = null)
        val match = BillMatcher.findMatchingOpenBill(1000.0, "Axis Bank", listOf(b))
        assertNull(match)
    }

    @Test
    fun `picks first matching bill from multiple candidates`() {
        val older = bill(1, "Axis Bank", totalDue = 1000.0)
        val newer = bill(2, "Axis Bank", totalDue = 1000.0)
        val match = BillMatcher.findMatchingOpenBill(1000.0, "Axis Bank", listOf(older, newer))
        assertEquals(1L, match?.id)
    }

    @Test
    fun `merchant substring match works in either direction`() {
        // billerName="Axis Bank", merchant="Axis" — merchant is a substring of biller
        val b1 = bill(1, "Axis Bank", totalDue = 1000.0)
        assertEquals(1L, BillMatcher.findMatchingOpenBill(1000.0, "Axis", listOf(b1))?.id)
        // billerName="ENBD", merchant="ENBD PAYMENTS" — biller is a substring of merchant
        val b2 = bill(2, "ENBD", totalDue = 500.0)
        assertEquals(2L, BillMatcher.findMatchingOpenBill(500.0, "ENBD PAYMENTS", listOf(b2))?.id)
    }
}
