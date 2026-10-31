package com.expenseanalyst.feature.notification.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.domain.util.BillReminderPlanner
import com.expenseanalyst.feature.notification.service.BillReminderNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Runs once a day around 12:00 (BillReminderScheduler). Which bills qualify is decided in
 * :domain (BillReminderPlanner) so it can be unit-tested; this only reads and posts.
 */
@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val billRepository: BillRepository,
    private val pendingRepository: PendingNotificationRepository,
    private val preferences: AppPreferencesRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!preferences.isBillRemindersEnabled().first()) return Result.success()
        val context = applicationContext

        val waiting = pendingRepository.getAll().first()
            .count { it.pendingType == "BILL" || it.pendingType == "BILL_REMINDER" }
        if (waiting > 0) {
            BillReminderNotification.postPendingStatements(context, waiting)
        } else {
            BillReminderNotification.cancelPendingStatements(context)
        }

        val bills = billRepository.getBills().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        BillReminderPlanner.remindersFor(bills, today).forEach {
            BillReminderNotification.postForBill(context, it)
        }
        // A bill settled since yesterday shouldn't keep yesterday's reminder in the tray
        bills.filter { it.status == BillStatus.SETTLED || it.isDeleted }
            .forEach { BillReminderNotification.cancelForBill(context, it.id) }
        return Result.success()
    }
}
