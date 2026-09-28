package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.PaymentMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaymentMethodInferenceTest {

    @Test
    fun `a card or wallet account answers the payment method`() {
        assertEquals(PaymentMethod.CREDIT_CARD, PaymentMethodInference.fromAccountType(AccountType.CREDIT_CARD))
        assertEquals(PaymentMethod.DEBIT_CARD, PaymentMethodInference.fromAccountType(AccountType.DEBIT_CARD))
        assertEquals(PaymentMethod.DEBIT_CARD, PaymentMethodInference.fromAccountType(AccountType.FOREX_CARD))
        assertEquals(PaymentMethod.WALLET, PaymentMethodInference.fromAccountType(AccountType.WALLET))
    }

    @Test
    fun `a bank account does not — it could be UPI, net banking or a card`() {
        assertEquals(null, PaymentMethodInference.fromAccountType(AccountType.SAVINGS))
        assertEquals(null, PaymentMethodInference.fromAccountType(AccountType.CURRENT))
        assertEquals(null, PaymentMethodInference.fromAccountType(AccountType.OTHER))
        assertEquals(null, PaymentMethodInference.fromAccountType(null))
    }
}
