package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BillPaymentPickerTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_794_000_000_000L
    private val bills = Category(1, "Bills", "x", "#000000", true, 0)

    private fun exp(
        id: Long, amount: Double, daysAgo: Long = 1, currency: String = "SAR", billId: Long? = null,
        type: TransactionType = TransactionType.EXPENSE, deleted: Boolean = false
    ) = Expense(
        id = id, amount = amount, currencyCode = currency, homeAmount = amount, exchangeRate = 1.0,
        description = "", category = bills, paymentMethod = PaymentMethod.CREDIT_CARD, transactionType = type,
        date = Instant.fromEpochMilliseconds(now - daysAgo * day), merchantName = "SAUDI ELECTRICITY COMP",
        sourceType = SourceType.NOTIFICATION_AUTO, billId = billId, isDeleted = deleted
    )

    private fun bill(id: Long, total: Double, status: BillStatus = BillStatus.PENDING, currency: String = "SAR") = Bill(
        id = id, billerName = "Saudi Energy", totalDue = total, currencyCode = currency,
        status = status, sourceType = SourceType.SMS_AUTO, createdAtMillis = 0L
    )

    @Test
    fun `an expense of exactly an open bill's amount hints that bill`() {
        val all = listOf(bill(1, 185.82), bill(2, 185.82, BillStatus.SETTLED), bill(3, 185.82, currency = "INR"), bill(4, 90.0))
        assertEquals(listOf(1L), BillPaymentPicker.sameAmountOpenBills(exp(10, 185.82), all).map { it.id })
        // already on a bill, a transfer, or a different amount — no hint
        assertTrue(BillPaymentPicker.sameAmountOpenBills(exp(10, 185.82, billId = 9), all).isEmpty())
        assertTrue(BillPaymentPicker.sameAmountOpenBills(exp(10, 185.82, type = TransactionType.TRANSFER), all).isEmpty())
        assertTrue(BillPaymentPicker.sameAmountOpenBills(exp(10, 185.0), all).isEmpty())
    }

    @Test
    fun `mark as paid lists unlinked expenses of the last 60 days, exact amount first then newest`() {
        val b = bill(1, 185.82)
        val rows = listOf(
            exp(1, 50.0, daysAgo = 1), exp(2, 185.82, daysAgo = 10), exp(3, 70.0, daysAgo = 3),
            exp(4, 185.82, daysAgo = 61), exp(5, 30.0, billId = 7), exp(6, 30.0, currency = "INR"),
            exp(7, 30.0, deleted = true), exp(8, 40.0, type = TransactionType.INCOME),
            exp(9, 99.0, daysAgo = 2, type = TransactionType.PAYMENT)
        )
        assertEquals(listOf(2L, 1L, 9L, 3L), BillPaymentPicker.candidates(b, rows, now).map { it.id })
    }
}
