package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SpendClassifierTest {

    private fun category(name: String) =
        Category(id = 1, name = name, iconName = "x", colorHex = "#000000", isDefault = true, sortOrder = 0)

    private fun expense(
        amount: Double = 100.0,
        homeAmount: Double? = null,
        type: TransactionType = TransactionType.EXPENSE,
        categoryName: String = "Shopping",
        classification: TransferClassification? = null,
        loanId: Long? = null
    ) = Expense(
        id = 0,
        amount = amount,
        currencyCode = "SAR",
        homeAmount = homeAmount ?: amount,
        exchangeRate = 1.0,
        description = "",
        category = category(categoryName),
        paymentMethod = PaymentMethod.CREDIT_CARD,
        transactionType = type,
        date = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        merchantName = "Someone",
        sourceType = SourceType.MANUAL,
        transferClassification = classification,
        loanId = loanId
    )

    // ── isSpend: every TransactionType × TransferClassification combination ──

    @ParameterizedTest(name = "{0} + {1} -> isSpend={2}")
    @CsvSource(
        // Only an EXPENSE, or a TRANSFER the user marked EXTERNAL, is spending.
        "EXPENSE,          , true",
        "EXPENSE,  EXTERNAL, true",
        "EXPENSE,  OWN_ACCOUNT, true",
        "EXPENSE,  EXTERNAL_IN, true",
        "TRANSFER,         , false",
        "TRANSFER, EXTERNAL, true",
        "TRANSFER, OWN_ACCOUNT, false",
        "TRANSFER, EXTERNAL_IN, false",
        "INCOME,           , false",
        "INCOME,   EXTERNAL, false",
        "INCOME,   OWN_ACCOUNT, false",
        "INCOME,   EXTERNAL_IN, false",
        "PAYMENT,          , false",
        "PAYMENT,  EXTERNAL, false",
        "PAYMENT,  OWN_ACCOUNT, false",
        "PAYMENT,  EXTERNAL_IN, false"
    )
    fun `isSpend across every type and classification`(
        type: TransactionType,
        classification: TransferClassification?,
        expected: Boolean
    ) {
        assertEquals(expected, SpendClassifier.isSpend(expense(type = type, classification = classification)))
    }

    @Test
    fun `an unclassified transfer is neither spend nor received`() {
        val row = expense(type = TransactionType.TRANSFER, classification = null)
        assertFalse(SpendClassifier.isSpend(row))
        assertFalse(SpendClassifier.isReceived(row))
        assertTrue(SpendClassifier.isUnclassifiedTransfer(row))
    }

    @Test
    fun `an inbound transfer counts as received, not spend`() {
        val row = expense(type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL_IN)
        assertFalse(SpendClassifier.isSpend(row))
        assertTrue(SpendClassifier.isReceived(row))
    }

    @Test
    fun `a refund is income but not received`() {
        val row = expense(type = TransactionType.INCOME, categoryName = "Refund")
        assertTrue(SpendClassifier.isRefund(row))
        assertFalse(SpendClassifier.isReceived(row))
    }

    // ── homeValue ──

    @Test
    fun `homeValue falls back to the original amount when homeAmount is null`() {
        val row = expense(amount = 58.0).copy(homeAmount = null)
        assertEquals(58.0, SpendClassifier.homeValue(row))
    }

    // ── spendBreakdown ──

    @Test
    fun `spendBreakdown nets refunds and adds external transfers`() {
        val rows = listOf(
            expense(amount = 300.0),
            expense(amount = 100.0),
            expense(amount = 4000.0, type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL),
            expense(amount = 50.0, type = TransactionType.INCOME, categoryName = "Refund")
        )

        val b = SpendClassifier.spendBreakdown(rows)

        assertEquals(400.0, b.expenseTotal)
        assertEquals(2, b.expenseCount)
        assertEquals(4000.0, b.externalTransferTotal)
        assertEquals(1, b.externalTransferCount)
        assertEquals(50.0, b.refundTotal)
        assertEquals(400.0 + 4000.0 - 50.0, b.net)
    }

    @Test
    fun `spendBreakdown reports own and unclassified transfers but excludes them from net`() {
        val rows = listOf(
            expense(amount = 200.0),
            expense(amount = 5000.0, type = TransactionType.TRANSFER, classification = TransferClassification.OWN_ACCOUNT),
            expense(amount = 4000.0, type = TransactionType.TRANSFER, classification = null)
        )

        val b = SpendClassifier.spendBreakdown(rows)

        assertEquals(5000.0, b.ownTransferTotal)
        assertEquals(1, b.ownTransferCount)
        assertEquals(4000.0, b.unclassifiedTransferTotal)
        assertEquals(1, b.unclassifiedTransferCount)
        // The regression this feature exists to prevent: neither may move the headline figure.
        assertEquals(200.0, b.net)
    }

    @Test
    fun `net floors at zero when refunds exceed spending`() {
        val rows = listOf(
            expense(amount = 10.0),
            expense(amount = 500.0, type = TransactionType.INCOME, categoryName = "Refund")
        )
        assertEquals(0.0, SpendClassifier.spendBreakdown(rows).net)
    }

    @Test
    fun `spendBreakdown of an empty list is all zero`() {
        val b = SpendClassifier.spendBreakdown(emptyList())
        assertEquals(0.0, b.net)
        assertEquals(0, b.expenseCount)
    }


    // ── Loan legs ──

    @ParameterizedTest(name = "loan leg {0} + {1} counts toward neither total")
    @CsvSource(
        "EXPENSE,          ",
        "TRANSFER, EXTERNAL",
        "TRANSFER, EXTERNAL_IN",
        "TRANSFER, OWN_ACCOUNT",
        "TRANSFER,         ",
        "INCOME,           ",
        "PAYMENT,          "
    )
    fun `a row linked to a loan counts toward neither Spent nor Received`(
        type: TransactionType,
        classification: TransferClassification?
    ) {
        val row = expense(type = type, classification = classification, loanId = 7)
        assertFalse(SpendClassifier.isSpend(row))
        assertFalse(SpendClassifier.isReceived(row))
        assertFalse(SpendClassifier.isRefund(row))
        assertFalse(SpendClassifier.isOwnTransfer(row))
        assertFalse(SpendClassifier.isUnclassifiedTransfer(row))
        assertTrue(SpendClassifier.isLoanLeg(row))
    }

    @Test
    fun `a loan repayment is identified by income type or an inbound classification`() {
        assertTrue(SpendClassifier.isLoanRepayment(expense(type = TransactionType.INCOME, loanId = 1)))
        assertTrue(
            SpendClassifier.isLoanRepayment(
                expense(type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL_IN, loanId = 1)
            )
        )
        assertFalse(
            SpendClassifier.isLoanRepayment(
                expense(type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL, loanId = 1)
            )
        )
        // Not a loan leg at all, so never a repayment even though it is inbound.
        assertFalse(SpendClassifier.isLoanRepayment(expense(type = TransactionType.INCOME, loanId = null)))
    }

    @Test
    fun `a round trip leaves both totals unchanged`() {
        // Lent SAR 4,000 out, got INR 102,104 (≈ SAR 4,607.58) back. Neither figure may move.
        val base = listOf(expense(amount = 200.0))
        val trip = listOf(
            expense(amount = 4000.0, type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL, loanId = 3),
            expense(
                amount = 102104.0, homeAmount = 4607.58, type = TransactionType.TRANSFER,
                classification = TransferClassification.EXTERNAL_IN, loanId = 3
            )
        )
        assertEquals(SpendClassifier.spendBreakdown(base).net, SpendClassifier.spendBreakdown(base + trip).net)
        assertEquals(SpendClassifier.receivedBreakdown(base).net, SpendClassifier.receivedBreakdown(base + trip).net)
    }

    @Test
    fun `a loan split across several transfers is reported as one set-aside amount`() {
        val legs = listOf(1500.0, 2500.0, 3000.0).map {
            expense(amount = it, type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL, loanId = 9)
        }
        val b = SpendClassifier.spendBreakdown(legs)
        assertEquals(7000.0, b.loanOutTotal)
        assertEquals(3, b.loanOutCount)
        assertEquals(0.0, b.net)
    }

    @Test
    fun `repayments are reported on the received side and never count toward it`() {
        val repay = expense(amount = 500.0, type = TransactionType.INCOME, loanId = 4)
        val b = SpendClassifier.receivedBreakdown(listOf(repay))
        assertEquals(500.0, b.loanRepaidTotal)
        assertEquals(1, b.loanRepaidCount)
        assertEquals(0.0, b.net)
        // ...and the outbound side of the same loan is not double-reported here.
        assertEquals(0, SpendClassifier.receivedBreakdown(
            listOf(expense(type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL, loanId = 4))
        ).loanRepaidCount)
    }

    // ── receivedBreakdown ──

    @Test
    fun `receivedBreakdown excludes refunds and includes inbound transfers`() {
        val rows = listOf(
            expense(amount = 3720.0, type = TransactionType.INCOME, categoryName = "Salary"),
            expense(amount = 4607.58, type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL_IN),
            expense(amount = 50.0, type = TransactionType.INCOME, categoryName = "Refund")
        )

        val b = SpendClassifier.receivedBreakdown(rows)

        assertEquals(3720.0, b.incomeTotal)
        assertEquals(4607.58, b.transferInTotal)
        assertEquals(50.0, b.refundTotal)
        // Refunds net out of Spent instead, so they must not inflate Received.
        assertEquals(3720.0 + 4607.58, b.net)
    }

    @Test
    fun `an unclassified transfer contributes to neither breakdown`() {
        val rows = listOf(expense(amount = 4000.0, type = TransactionType.TRANSFER, classification = null))
        assertEquals(0.0, SpendClassifier.spendBreakdown(rows).net)
        assertEquals(0.0, SpendClassifier.receivedBreakdown(rows).net)
    }
}
