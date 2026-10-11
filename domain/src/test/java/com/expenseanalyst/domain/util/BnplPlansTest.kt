package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.BnplChargeLink
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BnplPlansTest {

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_780_000_000_000L

    private fun row(
        id: Long,
        amount: Double,
        type: TransactionType,
        merchant: String,
        atDay: Int,
        provider: String? = null,
        count: Int? = null,
        purchaseId: Long? = null,
        deleted: Boolean = false
    ) = Expense(
        id = id,
        amount = amount,
        currencyCode = "SAR",
        homeAmount = amount,
        exchangeRate = 1.0,
        description = "",
        category = Category(id = 1, name = "Shopping", iconName = "x", colorHex = "#000000", isDefault = true, sortOrder = 0),
        paymentMethod = PaymentMethod.CREDIT_CARD,
        transactionType = type,
        date = Instant.fromEpochMilliseconds(t0 + atDay * day),
        merchantName = merchant,
        sourceType = SourceType.SMS_AUTO,
        isDeleted = deleted,
        bnplPurchaseId = purchaseId,
        bnplProvider = provider,
        bnplInstalmentCount = count
    )

    private fun purchase(id: Long, total: Double, provider: String, atDay: Int, count: Int? = null) =
        row(id, total, TransactionType.PAYMENT, "Shop $id", atDay, provider, count)

    private fun charge(id: Long, amount: Double, atDay: Int, purchaseId: Long? = null, name: String = "Tamara") =
        row(id, amount, TransactionType.EXPENSE, name, atDay, purchaseId = purchaseId)

    @Test
    fun `a plan counts its matched charges and infers the count from them`() {
        val plans = BnplPlans.build(
            listOf(
                purchase(1, 599.0, "Tabby", 0),
                charge(10, 149.75, 0, purchaseId = 1, name = "Shop 1 1/4 · Tabby"),
                charge(11, 149.75, 30, purchaseId = 1, name = "Shop 1 2/4 · Tabby")
            ),
            emptyList()
        )
        val plan = plans.single()
        assertEquals(4, plan.instalmentCount)
        assertEquals(2, plan.paidCount)
        assertEquals(299.5, plan.paidAmount, 0.001)
        assertEquals(299.5, plan.remaining, 0.001)
        assertFalse(plan.isComplete)
    }

    @Test
    fun `a combined charge pays one instalment of each plan it is linked to`() {
        val rows = listOf(
            purchase(1, 2397.99, "Tamara", 0, count = 3),  // 799.33
            purchase(2, 1549.53, "Tamara", 5, count = 3),  // 516.51
            charge(20, 1315.84, 40)
        )
        val plans = BnplPlans.build(rows, listOf(BnplChargeLink(20, 1), BnplChargeLink(20, 2))).associateBy { it.purchase.id }
        assertEquals(1, plans.getValue(1).paidCount)
        assertEquals(799.33, plans.getValue(1).paidAmount, 0.01)
        assertEquals(516.51, plans.getValue(2).paidAmount, 0.01)
        assertTrue(plans.getValue(1).payments.single().isCombined)
    }

    @Test
    fun `a plan is complete once every instalment is paid`() {
        val plan = BnplPlans.build(
            listOf(
                purchase(1, 300.0, "Tabby", 0),
                charge(10, 100.0, 0, 1), charge(11, 100.0, 30, 1), charge(12, 100.0, 60, 1)
            ),
            emptyList()
        ).single()
        assertTrue(plan.isComplete)
        assertEquals(0.0, plan.remaining, 0.001)
    }

    @Test
    fun `deleted rows and unstamped payments are not plans`() {
        val plans = BnplPlans.build(
            listOf(
                purchase(1, 300.0, "Tabby", 0),
                row(2, 300.0, TransactionType.PAYMENT, "Card bill", 0),           // no provider
                row(3, 300.0, TransactionType.PAYMENT, "Gone", 0, provider = "Tamara", deleted = true),
                charge(10, 100.0, 0, purchaseId = 1, name = "x").copy(isDeleted = true)
            ),
            emptyList()
        )
        assertEquals(listOf(1L), plans.map { it.purchase.id })
        assertEquals(0, plans.single().paidCount)
        assertNull(plans.single().instalmentCount)
    }

    @Test
    fun `candidates are open plans of the same provider bought by the charge date`() {
        val plans = BnplPlans.build(
            listOf(
                purchase(1, 300.0, "Tamara", 0, count = 3),
                purchase(2, 300.0, "Tabby", 0, count = 3),
                purchase(3, 300.0, "Tamara", 50, count = 3),                      // bought after the charge
                purchase(4, 200.0, "Tamara", 0, count = 2),
                charge(40, 100.0, 1, 4, "x"), charge(41, 100.0, 31, 4, "x")       // plan 4 complete
            ),
            emptyList()
        )
        val candidates = BnplPlans.candidatesFor(charge(20, 100.0, 40), plans)
        assertEquals(listOf(1L), candidates.map { it.purchase.id })
    }

    @Test
    fun `suggest finds the plans whose instalments add up, fewest first`() {
        val plans = BnplPlans.build(
            listOf(
                purchase(1, 2397.99, "Tamara", 0, count = 3),  // 799.33
                purchase(2, 1549.53, "Tamara", 5, count = 3),  // 516.51
                purchase(3, 900.0, "Tamara", 6, count = 3)     // 300
            ),
            emptyList()
        )
        assertEquals(setOf(1L, 2L), BnplPlans.suggest(1315.84, plans))
        assertEquals(setOf(3L), BnplPlans.suggest(300.0, plans))
        assertEquals(emptySet<Long>(), BnplPlans.suggest(1234.0, plans))
    }

    @Test
    fun `only bare Tabby or Tamara charges are provider charges`() {
        assertEquals("Tamara", BnplPlans.providerOfCharge(charge(1, 10.0, 0, name = " tamara ")))
        assertNull(BnplPlans.providerOfCharge(charge(1, 10.0, 0, name = "Shop 1/3 · Tamara")))
        assertNull(BnplPlans.providerOfCharge(row(1, 10.0, TransactionType.INCOME, "Tamara", 0)))
    }
}
