package com.expenseanalyst.feature.notification.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.expenseanalyst.domain.model.Category
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

    /** Category fields threaded through so the reply/confirmation notifications keep the icon. */
    const val EXTRA_CATEGORY_COLOR = "notif_category_color"
    const val EXTRA_CATEGORY_ICON = "notif_category_icon"
    const val EXTRA_CATEGORY_NAME = "notif_category_name"

    /** How long the post-reply confirmation stays in the shade before the system removes it. */
    private const val NOTE_CONFIRM_TIMEOUT_MS = 4_000L

    /** Pixel size of the rasterized category badge passed to [NotificationCompat.Builder.setLargeIcon]. */
    private const val LARGE_ICON_SIZE_PX = 192

    private var nextNotifId = 2000

    /**
     * Posts a notification for an auto-saved expense. Tapping opens the expense detail screen;
     * the "Add note" action accepts an inline reply that is written to the expense description.
     */
    fun postForExpense(context: Context, parsed: ParsedTransaction, expenseId: Long, category: Category) {
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

        postWithNoteAction(
            context, notifId, expenseId, title, bodyText,
            category.colorHex, category.iconName, category.name
        )
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
        body: String,
        categoryColorHex: String?,
        categoryIconName: String?,
        categoryName: String?
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)
        postWithNoteAction(context, notifId, expenseId, title, body, categoryColorHex, categoryIconName, categoryName)
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
        note: String,
        categoryColorHex: String?,
        categoryIconName: String?,
        categoryName: String?
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val icon = categoryLargeIcon(context, categoryColorHex, categoryIconName, categoryName)
        val notification = baseBuilder(context, title, note, contentIntentFor(context, expenseId, notifId), icon)
            .setRemoteInputHistory(arrayOf(note))
            .setTimeoutAfter(NOTE_CONFIRM_TIMEOUT_MS)
            .setOnlyAlertOnce(true)
            .build()

        notify(context, notifId, notification)
    }

    /** Reports that the note could not be saved, then self-dismisses. */
    fun postNoteFailed(
        context: Context,
        notifId: Int,
        title: String,
        categoryColorHex: String?,
        categoryIconName: String?,
        categoryName: String?
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(manager)

        val icon = categoryLargeIcon(context, categoryColorHex, categoryIconName, categoryName)
        val notification = baseBuilder(
            context,
            title,
            "Couldn't save note — this expense no longer exists",
            contentIntent = null,
            largeIcon = icon
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
        body: String,
        categoryColorHex: String?,
        categoryIconName: String?,
        categoryName: String?
    ) {
        val icon = categoryLargeIcon(context, categoryColorHex, categoryIconName, categoryName)
        val builder = baseBuilder(context, title, body, contentIntentFor(context, expenseId, notifId), icon)
        buildNoteAction(context, notifId, expenseId, title, body, categoryColorHex, categoryIconName, categoryName)
            ?.let(builder::addAction)
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
        body: String,
        categoryColorHex: String?,
        categoryIconName: String?,
        categoryName: String?
    ): NotificationCompat.Action? {
        val replyIntent = Intent(context, NoteReplyReceiver::class.java).apply {
            action = ACTION_ADD_NOTE
            putExtra(EXTRA_EXPENSE_ID, expenseId)
            putExtra(EXTRA_NOTIF_ID, notifId)
            putExtra(EXTRA_NOTIF_TITLE, title)
            putExtra(EXTRA_NOTIF_BODY, body)
            putExtra(EXTRA_CATEGORY_COLOR, categoryColorHex)
            putExtra(EXTRA_CATEGORY_ICON, categoryIconName)
            putExtra(EXTRA_CATEGORY_NAME, categoryName)
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
        contentIntent: PendingIntent?,
        largeIcon: Bitmap? = null
    ): NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(contentIntent)
        .setLargeIcon(largeIcon)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    /**
     * Renders a circular category badge for the notification's large icon: the category's
     * [Category.colorHex] as the fill, with either its mapped glyph ([CategoryNotificationIcon])
     * or — for a custom category outside that curated set — the category name's first letter in
     * white, matching the same fallback used for its in-app avatar.
     *
     * Never throws: a missing/invalid color or an unmapped icon name degrades gracefully rather
     * than dropping the notification.
     */
    private fun categoryLargeIcon(
        context: Context,
        colorHex: String?,
        iconName: String?,
        categoryName: String?
    ): Bitmap {
        val size = LARGE_ICON_SIZE_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = colorHex?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: Color.GRAY
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        val resId = iconName?.let(CategoryNotificationIcon::drawableFor)
        if (resId != null) {
            val inset = (size * 0.22f).toInt()
            ContextCompat.getDrawable(context, resId)?.apply {
                setBounds(inset, inset, size - inset, size - inset)
                draw(canvas)
            }
        } else {
            val letter = categoryName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = size * 0.42f
                typeface = Typeface.DEFAULT_BOLD
            }
            val baselineY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(letter, size / 2f, baselineY, textPaint)
        }

        return bitmap
    }

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
