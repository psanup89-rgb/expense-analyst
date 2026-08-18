package com.expenseanalyst.feature.notification.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteInput
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
    const val ACTION_OPEN_EXPENSE_DETAIL = "com.expenseanalyst.ACTION_OPEN_EXPENSE_DETAIL"
    const val EXTRA_AMOUNT = "notif_amount"
    const val EXTRA_CURRENCY = "notif_currency"
    const val EXTRA_MERCHANT = "notif_merchant"
    const val EXTRA_TYPE = "notif_type"
    const val EXTRA_ACCOUNT = "notif_account"
    const val EXTRA_PAYMENT_METHOD = "notif_payment_method"
    const val EXTRA_PENDING_ID = "notif_pending_id"
    const val EXTRA_EXPENSE_ID = "notif_expense_id"

    /** Broadcast action for the inline "Add note" RemoteInput reply. */
    const val ACTION_ADD_NOTE = "com.expenseanalyst.ACTION_ADD_NOTE"

    /** RemoteInput result key carrying the typed note. */
    const val KEY_NOTE_REPLY = "key_note_reply"
    const val EXTRA_NOTIF_ID = "notif_id"
    const val EXTRA_NOTIF_TITLE = "notif_title"
    const val EXTRA_NOTIF_BODY = "notif_body"

    /** How long the post-reply confirmation stays in the shade before the system removes it. */
    private const val NOTE_CONFIRM_TIMEOUT_MS = 4_000L

    private var nextNotifId = 2000

    /**
     * Posts a notification for an auto-saved expense. Tapping opens the expense detail screen;
     * the "Add note" action accepts an inline reply that is written to the expense description.
     */
    fun postForExpense(context: Context, parsed: ParsedTransaction, expenseId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val notifId = expenseId.toInt().coerceAtLeast(1)

        val direction = when (parsed.type) {
            TransactionDirection.DEBIT -> "Saved"
            TransactionDirection.TRANSFER -> "Transfer saved"
            else -> "Income saved"
        }
        val amountStr = "%.2f %s".format(parsed.amount, parsed.currencyCode)
        val title = "$direction · $amountStr"
        val bodyText = parsed.merchant?.takeIf { it.isNotBlank() }
            ?: parsed.bankName.takeIf { it != "Unknown Bank" }
            ?: "Tap to review"

        postWithNoteAction(context, notifId, expenseId, title, bodyText)
    }

    /**
     * Re-posts the original notification unchanged. Used when an inline reply arrives blank:
     * the system leaves an indefinite progress spinner on the notification until the app
     * updates or cancels it, so this clears the spinner and restores the "Add note" action.
     */
    fun repostForNoteRetry(
        context: Context,
        notifId: Int,
        expenseId: Long,
        title: String,
        body: String
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)
        postWithNoteAction(context, notifId, expenseId, title, body)
    }

    /**
     * Confirms a saved note, then lets the system dismiss the notification after
     * [NOTE_CONFIRM_TIMEOUT_MS]. Carries no reply action: a second note would be a replace,
     * which belongs in the app's edit screen.
     */
    fun postNoteSaved(
        context: Context,
        notifId: Int,
        expenseId: Long,
        title: String,
        note: String
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val notification = baseBuilder(context, title, note, contentIntentFor(context, expenseId, notifId))
            .setRemoteInputHistory(arrayOf(note))
            .setTimeoutAfter(NOTE_CONFIRM_TIMEOUT_MS)
            .setOnlyAlertOnce(true)
            .build()

        notify(context, notifId, notification)
    }

    /** Reports that the note could not be saved, then self-dismisses. */
    fun postNoteFailed(context: Context, notifId: Int, title: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val notification = baseBuilder(
            context,
            title,
            "Couldn't save note — this expense no longer exists",
            contentIntent = null
        )
            .setTimeoutAfter(NOTE_CONFIRM_TIMEOUT_MS)
            .setOnlyAlertOnce(true)
            .build()

        notify(context, notifId, notification)
    }

    /** Removes a notification outright. Last resort when there is nothing useful to show. */
    fun cancel(context: Context, notifId: Int) {
        NotificationManagerCompat.from(context).cancel(notifId)
    }

    /** Legacy method kept for backward compat (old notifications still in tray). */
    fun post(context: Context, parsed: ParsedTransaction, pendingId: Long? = null) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

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

        val notifId = pendingId?.toInt() ?: nextNotifId++
        val pendingIntent = PendingIntent.getActivity(
            context, notifId, launchIntent,
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

        postNotification(context, notifId, title, bodyText, pendingIntent)
    }

    // ── Auto-saved expense notification internals ────────────────────────────

    /** Builds and posts the standard auto-save notification with the inline note action. */
    private fun postWithNoteAction(
        context: Context,
        notifId: Int,
        expenseId: Long,
        title: String,
        body: String
    ) {
        val builder = baseBuilder(context, title, body, contentIntentFor(context, expenseId, notifId))
        buildNoteAction(context, notifId, expenseId, title, body)?.let(builder::addAction)
        notify(context, notifId, builder.build())
    }

    /**
     * The tap-the-body intent. Shared by every posting path so the route into
     * [ACTION_OPEN_EXPENSE_DETAIL] cannot drift between them.
     */
    private fun contentIntentFor(context: Context, expenseId: Long, notifId: Int): PendingIntent? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                action = ACTION_OPEN_EXPENSE_DETAIL
                putExtra(EXTRA_EXPENSE_ID, expenseId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            } ?: return null

        return PendingIntent.getActivity(
            context, notifId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Request code for the reply PendingIntent.
     *
     * The content intent already uses [notifId] itself as its request code. XOR-ing with
     * [Int.MIN_VALUE] is a bijection over Int that maps the always-positive notifId into the
     * negative range, so the two can never collide for any input. A collision here would
     * deliver a note to the wrong expense, hence the dedicated unit test.
     */
    internal fun replyRequestCode(notifId: Int): Int = notifId xor Int.MIN_VALUE

    /**
     * The inline-reply action.
     *
     * The PendingIntent must be mutable or [RemoteInput.getResultsFromIntent] returns null.
     * That is safe here because the intent is explicit — its component is pinned to
     * [NoteReplyReceiver], which since Android 12 cannot be overwritten via Intent.fillIn —
     * and the receiver is not exported.
     *
     * Extras carry only the amount/currency/merchant strings already visible on screen.
     * Never put raw SMS text or account identifiers here.
     */
    private fun buildNoteAction(
        context: Context,
        notifId: Int,
        expenseId: Long,
        title: String,
        body: String
    ): NotificationCompat.Action? {
        val replyIntent = Intent(context, NoteReplyReceiver::class.java).apply {
            action = ACTION_ADD_NOTE
            putExtra(EXTRA_EXPENSE_ID, expenseId)
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtra(EXTRA_NOTIF_TITLE, title)
            putExtra(EXTRA_NOTIF_BODY, body)
        }

        val replyPendingIntent = PendingIntentCompat.getBroadcast(
            context,
            replyRequestCode(notifId),
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT,
            true
        ) ?: return null

        val remoteInput = RemoteInput.Builder(KEY_NOTE_REPLY)
            .setLabel("Add a note")
            .build()

        return NotificationCompat.Action.Builder(R.drawable.ic_note_add, "Add note", replyPendingIntent)
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .setAllowGeneratedReplies(false)
            .build()
    }

    private fun baseBuilder(
        context: Context,
        title: String,
        body: String,
        contentIntent: PendingIntent?
    ): NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    private fun notify(context: Context, notifId: Int, notification: Notification) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(notifId, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS permission not granted — silently skipped
            }
        }
    }

    private fun ensureChannel(manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Transaction Alerts", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Alerts when a bank transaction is detected" }
        manager.createNotificationChannel(channel)
    }

    private fun postNotification(
        context: Context,
        notifId: Int,
        title: String,
        body: String,
        pendingIntent: PendingIntent
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(notifId, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS permission not granted — silently skipped
            }
        }
    }
}
