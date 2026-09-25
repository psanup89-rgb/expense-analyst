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

/**
 * What a [TransactionType.TRANSFER] row actually represents. Deliberately NOT a member of
 * [TransactionType]: that enum is decoded with a bare `valueOf` in ExpenseMapper, so a new
 * constant would hard-crash the list on a downgrade, and it would also erase the fact that the
 * row is a transfer (every consumer would need `type in setOf(TRANSFER, OWN_TRANSFER)`).
 *
 * A null classification means "not yet classified" and is excluded from every total — that is
 * the pre-classification behaviour, so upgrading moves no historical figure. The user classifies
 * a row explicitly; see [com.expenseanalyst.domain.util.SpendClassifier].
 *
 * [EXTERNAL_IN] exists because the parsers emit `TransactionDirection.TRANSFER` for *incoming*
 * internal transfers too (AlRajhiParser's "Credit Internal Transfer" / "Credit Transfer
 * Internal", StcBankParser's transfer branch), and direction is discarded at the
 * ParsedTransaction boundary — so a stored row cannot tell inbound from outbound. Without a
 * third option the user would be forced to label received money as spending. It is manual-only:
 * auto-classification never produces it.
 */
enum class TransferClassification(val label: String) {
    /** Money sent to someone else. Counts toward Spent, exactly like an EXPENSE. */
    EXTERNAL("Sent to someone else"),

    /** Moved between the user's own accounts. Never counts as spending. */
    OWN_ACCOUNT("Moved to my own account"),

    /** Money received from someone else. Counts toward Received. */
    EXTERNAL_IN("Received from someone else")
}

enum class SourceType {
    MANUAL, SMS_AUTO, NOTIFICATION_AUTO
}

enum class BillStatus {
    PENDING, PARTIAL, SETTLED
}

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}
