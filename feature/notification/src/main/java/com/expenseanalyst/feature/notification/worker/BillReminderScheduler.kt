package com.expenseanalyst.feature.notification.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Keeps one daily [BillReminderWorker] enqueued, first run at the next 12:00 local time (owner's
 * choice). Called on every app start; KEEP leaves an existing schedule alone. The Settings switch
 * is read by the worker itself, so turning reminders off or on needs no rescheduling here.
 */
object BillReminderScheduler {

    private const val WORK_NAME = "bill_reminders_daily"
    private val REMINDER_TIME: LocalTime = LocalTime.NOON

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(REMINDER_TIME).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun delayUntilNext(time: LocalTime): Duration {
        val now = ZonedDateTime.now()
        var next = now.with(time).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
