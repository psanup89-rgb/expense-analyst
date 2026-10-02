package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.util.BillSettlement
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The one path for tying a payment to a bill — from the expense detail sheet, the bill detail
 * unlink, manual add, and live capture's auto-link — so all four agree on two things:
 *  - the payment leaves Review for what the link answers (category, payment method);
 *  - the bill's status is worked out from every linked payment in the bill's currency
 *    ([BillSettlement]), not from the one payment being linked.
 */
class LinkBillPaymentUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val billRepository: BillRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository
) {

    suspend fun link(expenseId: Long, billId: Long) {
        val expense = expenseRepository.getExpenseById(expenseId).first() ?: return
        val previousBillId = expense.billId
        val accountType = expense.accountId?.let { accountRepository.getAccountById(it).first()?.accountType }
        expenseRepository.linkBillPayment(expenseId, billId, methodForBillPayment(accountType))
        if (previousBillId != null && previousBillId != billId) refreshStatus(previousBillId)
        refreshStatus(billId)
    }

    suspend fun unlink(expenseId: Long) {
        val billId = expenseRepository.getExpenseById(expenseId).first()?.billId ?: return
        expenseRepository.unlinkBillPayment(expenseId)
        refreshStatus(billId)
    }

    suspend fun refreshStatus(billId: Long) {
        val bill = billRepository.getBillById(billId).first() ?: return
        val payments = expenseRepository.getExpensesByBillId(billId).first()
        val status = BillSettlement.status(
            totalDue = bill.totalDue,
            billCurrency = bill.currencyCode,
            payments = payments,
            homeCurrency = currencyRepository.getHomeCurrency().first(),
            ratesByCode = currencyRepository.getRates().first().associateBy { it.currencyCode }
        )
        if (status != bill.status) billRepository.updateBill(bill.copy(status = status))
    }

    companion object {
        /**
         * A card bill paid from a bank account is a bank transfer — the same reading as an ACH
         * auto-debit. A card or wallet account answers it the usual way; unknown stays unknown.
         */
        fun methodForBillPayment(accountType: AccountType?): PaymentMethod? = when (accountType) {
            AccountType.SAVINGS, AccountType.CURRENT -> PaymentMethod.NET_BANKING
            AccountType.CREDIT_CARD -> PaymentMethod.CREDIT_CARD
            AccountType.DEBIT_CARD, AccountType.FOREX_CARD -> PaymentMethod.DEBIT_CARD
            AccountType.WALLET -> PaymentMethod.WALLET
            else -> null
        }
    }
}
