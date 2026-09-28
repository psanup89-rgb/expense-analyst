package com.expenseanalyst.domain.model

data class Bill(
    val id: Long = 0,
    val billerName: String,
    val accountId: Long? = null,
    val totalDue: Double? = null,
    val minimumDue: Double? = null,
    val currencyCode: String,
    val dueDateMillis: Long? = null,
    val statementPeriodStart: Long? = null,
    val statementPeriodEnd: Long? = null,
    val status: BillStatus,
    val sourceType: SourceType,
    val createdAtMillis: Long,
    val isDeleted: Boolean = false,
    val reference: String? = null,
    /**
     * How many reminders for this bill the user has linked to it (a biller re-sends the same
     * bill as "kindly remind you…", "will be suspended in 48 hours…"). Carried through every
     * full-row update — see BillRepositoryImpl's mapper.
     */
    val reminderCount: Int = 0,
    val lastReminderAtMillis: Long? = null
)
