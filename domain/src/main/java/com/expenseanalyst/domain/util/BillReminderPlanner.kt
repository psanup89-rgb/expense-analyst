package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

/**
 * Which saved bills get a reminder today (owner's rules, Oct 2026): every unsettled bill from
 * [LEAD_DAYS] days before its due date, then every day — overdue included — until it is settled.
 * A bill with no due date is reminded every day too, with a prompt to set one.
 *
 * Due dates are stored as UTC midnight of the calendar day (statement parsers and the Edit Bill
 * date picker both do this), so they are read back as a UTC date; "today" is the phone's own day.
 */
object BillReminderPlanner {

    const val LEAD_DAYS = 3

    /** [daysUntilDue] is negative when overdue, 0 on the due day, null when the bill has no due date. */
    data class Reminder(val bill: Bill, val daysUntilDue: Int?)

    fun remindersFor(bills: List<Bill>, today: LocalDate): List<Reminder> =
        bills
            .filter { !it.isDeleted && it.status != BillStatus.SETTLED }
            .mapNotNull { bill ->
                val due = bill.dueDateMillis ?: return@mapNotNull Reminder(bill, null)
                val days = today.daysUntil(dueDate(due))
                if (days <= LEAD_DAYS) Reminder(bill, days) else null
            }
            .sortedWith(compareBy(nullsLast()) { it.daysUntilDue })

    fun dueDate(dueMillis: Long): LocalDate =
        Instant.fromEpochMilliseconds(dueMillis).toLocalDateTime(TimeZone.UTC).date

    /** "Due in 2 days", "Due tomorrow", "Due today", "Overdue by 3 days", "No due date — tap to set one". */
    fun dueText(daysUntilDue: Int?): String = when {
        daysUntilDue == null -> "No due date — tap to set one"
        daysUntilDue < 0 -> "Overdue by ${days(-daysUntilDue)}"
        daysUntilDue == 0 -> "Due today"
        daysUntilDue == 1 -> "Due tomorrow"
        else -> "Due in ${days(daysUntilDue)}"
    }

    private fun days(n: Int) = if (n == 1) "1 day" else "$n days"
}
