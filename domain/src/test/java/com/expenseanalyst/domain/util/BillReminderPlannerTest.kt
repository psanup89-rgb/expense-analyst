package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BillReminderPlannerTest {

    private val today = LocalDate(2026, 10, 31)

    private fun bill(
        id: Long,
        due: LocalDate?,
        status: BillStatus = BillStatus.PENDING,
        deleted: Boolean = false
    ) = Bill(
        id = id, billerName = "Biller $id", totalDue = 100.0, currencyCode = "SAR",
        dueDateMillis = due?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        status = status, sourceType = SourceType.SMS_AUTO, createdAtMillis = 0L, isDeleted = deleted
    )

    private fun plan(vararg bills: Bill) =
        BillReminderPlanner.remindersFor(bills.toList(), today).map { it.bill.id to it.daysUntilDue }

    @Test
    fun `reminds from three days before the due date, not earlier`() {
        assertEquals(
            listOf(3L to 3),
            plan(bill(3, LocalDate(2026, 11, 3)), bill(4, LocalDate(2026, 11, 4)))
        )
    }

    @Test
    fun `keeps reminding on the due day and every day it is overdue`() {
        assertEquals(
            listOf(2L to -5, 1L to 0),
            plan(bill(1, today), bill(2, LocalDate(2026, 10, 26)))
        )
    }

    @Test
    fun `settled and deleted bills get no reminder, partial ones do`() {
        assertEquals(
            listOf(3L to 1),
            plan(
                bill(1, today, status = BillStatus.SETTLED),
                bill(2, today, deleted = true),
                bill(3, LocalDate(2026, 11, 1), status = BillStatus.PARTIAL)
            )
        )
    }

    @Test
    fun `a bill without a due date is reminded every day, listed last`() {
        assertEquals(listOf(2L to 2, 1L to null), plan(bill(1, null), bill(2, LocalDate(2026, 11, 2))))
    }

    @Test
    fun `due text`() {
        assertEquals("Due in 3 days", BillReminderPlanner.dueText(3))
        assertEquals("Due tomorrow", BillReminderPlanner.dueText(1))
        assertEquals("Due today", BillReminderPlanner.dueText(0))
        assertEquals("Overdue by 1 day", BillReminderPlanner.dueText(-1))
        assertEquals("Overdue by 4 days", BillReminderPlanner.dueText(-4))
        assertEquals("No due date — tap to set one", BillReminderPlanner.dueText(null))
    }
}
