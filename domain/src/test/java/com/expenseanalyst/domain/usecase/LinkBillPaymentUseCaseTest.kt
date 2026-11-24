package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The bill-status rules behind "Mark as paid" — in-memory state behind mocked repositories. */
class LinkBillPaymentUseCaseTest {

    private val bills = Category(1, "Bills", "x", "#000000", true, 0)
    private var bill = Bill(
        id = 1, billerName = "Saudi Energy", totalDue = 185.82, currencyCode = "SAR",
        status = BillStatus.PENDING, sourceType = SourceType.SMS_AUTO, createdAtMillis = 0L
    )
    private val rows = mutableMapOf<Long, Expense>()

    private fun expense(id: Long, amount: Double, daysApart: Long) = Expense(
        id = id, amount = amount, currencyCode = "SAR", homeAmount = amount, exchangeRate = 1.0, description = "",
        category = bills, paymentMethod = PaymentMethod.CREDIT_CARD, transactionType = TransactionType.EXPENSE,
        date = Instant.fromEpochMilliseconds(1_794_000_000_000L + daysApart * 5L * 24 * 60 * 60 * 1000),
        merchantName = "SAUDI ELECTRICITY COMP", sourceType = SourceType.NOTIFICATION_AUTO
    )

    private val useCase: LinkBillPaymentUseCase by lazy {
        val billRepo = mockk<BillRepository>(relaxed = true)
        every { billRepo.getBillById(1) } answers { flowOf(bill) }
        coEvery { billRepo.updateBill(any()) } answers { bill = firstArg() }
        val expenseRepo = mockk<ExpenseRepository>(relaxed = true)
        every { expenseRepo.getExpenseById(any()) } answers { flowOf(rows[firstArg()]) }
        every { expenseRepo.getExpensesByBillId(1) } answers { flowOf(rows.values.filter { it.billId == 1L }) }
        coEvery { expenseRepo.getExpensesSnapshot(any()) } answers { rows.values.toList() }
        coEvery { expenseRepo.linkBillPayment(any(), any(), any()) } answers {
            val id: Long = firstArg(); rows[id] = rows.getValue(id).copy(billId = secondArg()); 1
        }
        coEvery { expenseRepo.unlinkBillPayment(any()) } answers {
            val id: Long = firstArg(); rows[id] = rows.getValue(id).copy(billId = null); 1
        }
        val currencyRepo = mockk<CurrencyRepository>(relaxed = true)
        every { currencyRepo.getHomeCurrency() } returns flowOf("SAR")
        every { currencyRepo.getRates() } returns flowOf(emptyList())
        LinkBillPaymentUseCase(expenseRepo, billRepo, mockk<AccountRepository>(relaxed = true), currencyRepo)
    }

    @Test
    fun `a payment that covers the bill settles it without asking`() = runTest {
        rows[10] = expense(10, 185.82, 0)
        assertTrue(useCase.wouldSettle(10, 1))
        useCase.link(10, 1)
        assertEquals(BillStatus.SETTLED, bill.status)
    }

    @Test
    fun `a short payment closed fully stays settled when another payment is linked`() = runTest {
        rows[10] = expense(10, 100.0, 0)
        rows[11] = expense(11, 20.0, 3)
        assertFalse(useCase.wouldSettle(10, 1))
        useCase.link(10, 1, closeFully = true)
        assertEquals(BillStatus.SETTLED, bill.status)
        useCase.link(11, 1)
        assertEquals(BillStatus.SETTLED, bill.status)
    }

    @Test
    fun `kept partial stays partial, and unlinking recalculates`() = runTest {
        rows[10] = expense(10, 100.0, 0)
        useCase.link(10, 1)
        assertEquals(BillStatus.PARTIAL, bill.status)
        useCase.unlink(10)
        assertEquals(BillStatus.PENDING, bill.status)
    }
}
