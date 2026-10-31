package com.expenseanalyst.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import com.expenseanalyst.feature.notification.worker.BillReminderScheduler
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpenseAnalystApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Daily 12:00 bill reminders; KEEP leaves an existing schedule alone
        BillReminderScheduler.ensureScheduled(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
