package com.expenseanalyst.core.util

import kotlinx.datetime.*

object DateTimeUtil {

    fun now(): Instant = Clock.System.now()

    fun nowMillis(): Long = now().toEpochMilliseconds()

    fun fromMillis(millis: Long): Instant = Instant.fromEpochMilliseconds(millis)

    fun toLocalDate(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate {
        return instant.toLocalDateTime(timeZone).date
    }

    fun toLocalDateTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
        return instant.toLocalDateTime(timeZone)
    }

    fun formatDateHeader(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val date = instant.toLocalDateTime(timeZone).date

        return when {
            date == today -> "Today"
            date == today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
            else -> {
                val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
                "${month.take(3)} ${date.dayOfMonth}, ${date.year}"
            }
        }
    }

    fun formatTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val ldt = instant.toLocalDateTime(timeZone)
        val hour = if (ldt.hour == 0) 12 else if (ldt.hour > 12) ldt.hour - 12 else ldt.hour
        val amPm = if (ldt.hour < 12) "AM" else "PM"
        return "$hour:${ldt.minute.toString().padStart(2, '0')} $amPm"
    }

    fun startOfDay(date: LocalDate, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
        return date.atStartOfDayIn(timeZone)
    }

    fun endOfDay(date: LocalDate, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
        return date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).minus(1, DateTimeUnit.MILLISECOND)
    }

    fun addMonths(instant: Instant, months: Int, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
        val ldt = instant.toLocalDateTime(timeZone)
        val newDate = ldt.date.plus(months, DateTimeUnit.MONTH)
        return LocalDateTime(newDate, ldt.time).toInstant(timeZone)
    }
}
