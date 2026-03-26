package com.expenseanalyst.feature.notification.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expenseanalyst.feature.notification.R
import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import com.expenseanalyst.feature.notification.parser.TransactionDirection

/**
 * Posts an Android system notification when a transaction is detected,
 * so the user is alerted even when the app is not in the foreground.
 *
 * Tapping the notification opens the app's main activity with extras
 * that trigger navigation to the Add Expense screen pre-filled.
 */
object TransactionAlertNotification {

    const val CHANNEL_ID = "expense_analyst_alerts"
    const val ACTION_OPEN_ADD_EXPENSE = "com.expenseanalyst.ACTION_OPEN_ADD_EXPENSE"
    const val EXTRA_AMOUNT = "notif_amount"
    const val EXTRA_CURRENCY = "notif_currency"
    const val EXTRA_MERCHANT = "notif_merchant"
    const val EXTRA_TYPE = "notif_type"

    private var nextNotifId = 2000

    fun post(context: Context, parsed: ParsedTransaction) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Create channel (no-op if already exists)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transaction Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a bank transaction is detected"
        }
        manager.createNotificationChannel(channel)

        // Build tap intent — launches the app's launcher activity without importing it directly
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                action = ACTION_OPEN_ADD_EXPENSE
                putExtra(EXTRA_AMOUNT, parsed.amount)
                putExtra(EXTRA_CURRENCY, parsed.currencyCode)
                putExtra(EXTRA_MERCHANT, parsed.merchant)
                putExtra(EXTRA_TYPE, parsed.type.name)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            } ?: return

        val pendingIntent = PendingIntent.getActivity(
            context,
            nextNotifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val direction = if (parsed.type == TransactionDirection.DEBIT) "Spent" else "Received"
        val amountStr = "%.2f %s".format(parsed.amount, parsed.currencyCode)
        val title = "$direction $amountStr"
        val bodyText = parsed.merchant?.takeIf { it.isNotBlank() }
            ?: parsed.bankName.takeIf { it != "Unknown Bank" }
            ?: "Tap to add expense"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(nextNotifId++, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS permission not granted — notification silently skipped
            }
        }
    }
}
