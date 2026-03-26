package com.expenseanalyst.domain.model

enum class PaymentMethod {
    CASH, UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, OTHER
}

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER, PAYMENT
}

enum class SourceType {
    MANUAL, SMS_AUTO, NOTIFICATION_AUTO
}
