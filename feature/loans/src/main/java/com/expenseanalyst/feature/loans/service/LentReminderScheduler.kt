package com.expenseanalyst.feature.loans.service

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.expenseanalyst.feature.loans.worker.LentReminderWorker
import java.util.concurrent.TimeUnit

object LentReminderScheduler {

    private fun tag(lentId: Long) = "lent_reminder_$lentId"

    fun schedule(context: Context, lentId: Long, reminderAtMillis: Long) {
        val delayMs = reminderAtMillis - System.currentTimeMillis()
        if (delayMs <= 0) return

        cancel(context, lentId)

        val inputData = Data.Builder()
            .putLong("lent_id", lentId)
            .build()

        val request = OneTimeWorkRequestBuilder<LentReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(tag(lentId))
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(context: Context, lentId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag(lentId))
    }
}
