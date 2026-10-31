package com.expenseanalyst.feature.notification.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.domain.util.BillReminderPlanner
import com.expenseanalyst.feature.notification.R

/**
 * The daily bill reminders posted by [com.expenseanalyst.feature.notification.worker.BillReminderWorker]:
 * one summary while statements wait in Pending Bill Statements (tap → the inbox), and one per bill
 * that is due within three days, overdue or has no due date (tap → that bill). Own channel, so the
 * reminders can be muted without silencing transaction alerts.
 */
object BillReminderNotification {

    /** Read by MainActivity to open a bill's detail screen. */
    const val ACTION_OPEN_BILL_DETAIL = "com.expenseanalyst.ACTION_OPEN_BILL_DETAIL"
    const val EXTRA_BILL_ID = "bill_id"

    private const val CHANNEL_ID = "bill_reminders"
    // Clear of expense ids (the row id) and TransactionAlertNotification's bill offset (900,000).
    private const val PENDING_SUMMARY_ID = 1_999_999_999
    private const val BILL_ID_OFFSET = 1_500_000_000L
    private const val LEDGER_ACCENT = 0xFFD6F25C.toInt()

    fun postPendingStatements(context: Context, count: Int) {
        val intent = launchIntent(context)?.apply { action = TransactionAlertNotification.ACTION_OPEN_PENDING_INBOX }
            ?: return
        post(
            context,
            PENDING_SUMMARY_ID,
            title = if (count == 1) "1 bill statement to add" else "$count bill statements to add",
            body = "Waiting in Pending Bill Statements — tap to review",
            intent = intent
        )
    }

    fun cancelPendingStatements(context: Context) =
        NotificationManagerCompat.from(context).cancel(PENDING_SUMMARY_ID)

    fun postForBill(context: Context, reminder: BillReminderPlanner.Reminder) {
        val bill = reminder.bill
        val intent = launchIntent(context)?.apply {
            action = ACTION_OPEN_BILL_DETAIL
            putExtra(EXTRA_BILL_ID, bill.id)
        } ?: return
        val amount = bill.totalDue?.let { CurrencyFormatter.format(it, bill.currencyCode) } ?: "amount not set"
        post(
            context,
            billNotifId(bill.id),
            title = "$amount · ${bill.billerName}",
            body = BillReminderPlanner.dueText(reminder.daysUntilDue),
            intent = intent
        )
    }

    fun cancelForBill(context: Context, billId: Long) =
        NotificationManagerCompat.from(context).cancel(billNotifId(billId))

    private fun billNotifId(billId: Long) = (BILL_ID_OFFSET + billId).toInt()

    private fun launchIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    private fun post(context: Context, notifId: Int, title: String, body: String, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Bill Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily reminders for bills to add and bills coming due"
            }
        )
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(LEDGER_ACCENT)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(notifId, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS not granted
            }
        }
    }
}
