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
    const val EXTRA_ACCOUNT = "notif_account"
    const val EXTRA_PAYMENT_METHOD = "notif_payment_method"
    const val EXTRA_PENDING_ID = "notif_pending_id"

    private var nextNotifId = 2000

    fun post(context: Context, parsed: ParsedTransaction, pendingId: Long? = null) {
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
                val accountStr = parsed.accountLast4?.let { last4 ->
                    val bank = parsed.bankName.takeIf { it != "Unknown Bank" } ?: ""
                    if (bank.isNotBlank()) "$bank *$last4" else "*$last4"
                }
                putExtra(EXTRA_ACCOUNT, accountStr)
                putExtra(EXTRA_PAYMENT_METHOD, parsed.paymentMethodName)
                if (pendingId != null) putExtra(EXTRA_PENDING_ID, pendingId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            } ?: return

        // Use pendingId as the notification ID so it can be cancelled later by the same ID.
        // Fall back to incrementing counter only when there is no DB-backed pending record.
        val notifId = pendingId?.toInt() ?: nextNotifId++

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val direction = when (parsed.type) {
            TransactionDirection.DEBIT -> "Spent"
            TransactionDirection.TRANSFER -> "Transfer"
            else -> "Received"
        }
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
                NotificationManagerCompat.from(context).notify(notifId, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS permission not granted — notification silently skipped
            }
        }
    }
}
