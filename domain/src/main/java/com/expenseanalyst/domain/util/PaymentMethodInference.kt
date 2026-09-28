package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.PaymentMethod

/**
 * The payment method a message leaves unsaid, from the account it was matched to.
 *
 * Many card messages name only the network — Al Rajhi's "Online Purchase By:1234 ;Visa …" — so
 * the parser can't tell credit from debit and the expense was saved as "Other" and flagged for
 * review, even though the card's last four had already matched the user's credit-card account.
 * Only a card or wallet account answers the question; a savings/current account could have been
 * debited by UPI, net banking or a debit card, so it stays unknown. Used only when the parser
 * found nothing — an explicit method in the message (Apple Pay, UPI…) always wins.
 */
object PaymentMethodInference {

    fun fromAccountType(type: AccountType?): PaymentMethod? = when (type) {
        AccountType.CREDIT_CARD -> PaymentMethod.CREDIT_CARD
        // A forex card is a prepaid card spent like a debit card
        AccountType.DEBIT_CARD, AccountType.FOREX_CARD -> PaymentMethod.DEBIT_CARD
        AccountType.WALLET -> PaymentMethod.WALLET
        else -> null
    }
}
