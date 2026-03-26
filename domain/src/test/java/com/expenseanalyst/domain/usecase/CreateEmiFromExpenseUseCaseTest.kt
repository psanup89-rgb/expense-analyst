package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.EmiGroup
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.EmiRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateEmiFromExpenseUseCaseTest {

    private lateinit var emiRepository: EmiRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var useCase: CreateEmiFromExpenseUseCase

    private val testCategory = Category(id = 1, name = "Shopping", iconName = "shopping_cart", colorHex = "#FF0000", isDefault = false, sortOrder = 0)
    private val testExpense = Expense(
        id = 10L,
        amount = 12000.0,
        currencyCode = "INR",
        homeAmount = 12000.0,
        exchangeRate = 1.0,
        description = "iPhone purchase",
        category = testCategory,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        transactionType = TransactionType.EXPENSE,
        date = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        merchantName = "Apple Store",
        sourceType = SourceType.MANUAL
    )

    @BeforeEach
    fun setUp() {
        emiRepository = mockk()
        expenseRepository = mockk()
        useCase = CreateEmiFromExpenseUseCase(emiRepository, expenseRepository)

        coEvery { emiRepository.createEmiGroup(any()) } returns 42L
        coEvery { expenseRepository.updateExpense(any()) } returns Unit
        coEvery { expenseRepository.addExpense(any()) } returns 0L
    }

    @Test
    fun `invoke creates EMI group and N installments without interest`() = runTest {
        val groupId = useCase(testExpense, numberOfMonths = 6)

        assertEquals(42L, groupId)

        // First installment reuses original expense (updateExpense)
        coVerify(exactly = 1) { expenseRepository.updateExpense(any()) }
        // Remaining 5 are new entries
        coVerify(exactly = 5) { expenseRepository.addExpense(any()) }
    }

    @Test
    fun `invoke calculates correct installment amount without interest`() = runTest {
        val groupSlot = slot<EmiGroup>()
        coEvery { emiRepository.createEmiGroup(capture(groupSlot)) } returns 42L

        useCase(testExpense, numberOfMonths = 4)

        val group = groupSlot.captured
        assertEquals(3000.0, group.installmentAmount, 0.01)  // 12000 / 4
        assertEquals(4, group.numberOfInstallments)
        assertEquals(12000.0, group.totalAmount, 0.01)
    }

    @Test
    fun `invoke calculates correct installment with interest`() = runTest {
        val groupSlot = slot<EmiGroup>()
        coEvery { emiRepository.createEmiGroup(capture(groupSlot)) } returns 42L

        // 12% annual rate on 12000 for 12 months
        useCase(testExpense, numberOfMonths = 12, annualInterestRate = 12.0)

        val group = groupSlot.captured
        // Standard EMI formula: P=12000, r=0.01/month, n=12
        // EMI ≈ 1064.65
        assertEquals(1064.65, group.installmentAmount, 0.5)
        assertEquals(12.0, group.interestRate)
    }

    @Test
    fun `invoke throws for fewer than 2 months`() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { useCase(testExpense, numberOfMonths = 1) }
        }
    }

    @Test
    fun `invoke throws for more than 60 months`() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { useCase(testExpense, numberOfMonths = 61) }
        }
    }

    @Test
    fun `invoke throws for zero amount expense`() {
        val zeroAmountExpense = testExpense.copy(amount = 0.0)
        assertThrows(IllegalArgumentException::class.java) {
            runTest { useCase(zeroAmountExpense, numberOfMonths = 6) }
        }
    }
}
