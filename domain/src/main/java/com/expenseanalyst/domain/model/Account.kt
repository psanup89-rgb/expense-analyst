package com.expenseanalyst.domain.model

data class Account(
    val id: Long = 0,
    val bankName: String,
    val lastFour: String?,
    val accountType: AccountType,
    val displayName: String
)

enum class AccountType(val label: String) {
    SAVINGS("Savings"),
    CURRENT("Current"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    FOREX_CARD("Forex Card"),
    WALLET("Wallet"),
    OTHER("Account")
}
