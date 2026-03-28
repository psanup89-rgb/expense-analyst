package com.expenseanalyst.domain.model

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    NET_BANKING("Net Banking"),
    WALLET("Wallet"),
    APPLE_PAY("Apple Pay"),
    SAMSUNG_PAY("Samsung Pay"),
    GOOGLE_PAY("Google Pay"),
    OTHER("Other")
}

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER, PAYMENT
}

enum class SourceType {
    MANUAL, SMS_AUTO, NOTIFICATION_AUTO
}

enum class BillStatus {
    PENDING, PARTIAL, SETTLED
}
