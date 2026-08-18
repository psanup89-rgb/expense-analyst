package com.expenseanalyst.feature.notification.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles the inline "Add note" reply on an auto-saved transaction notification, writing the
 * typed text to the expense description without opening the app.
 *
 * After a RemoteInput reply the system shows an indefinite progress spinner and waits for the
 * app to either re-notify the same id or cancel it. **Every path through this receiver must
 * therefore end in a notify or a cancel**, or the spinner is left stuck until the user swipes
 * the notification away.
 */
@AndroidEntryPoint
class NoteReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var expenseRepository: ExpenseRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TransactionAlertNotification.ACTION_ADD_NOTE) return

        val notifIdExtra = intent.getIntExtra(TransactionAlertNotification.EXTRA_NOTIF_ID, 0)
        val expenseId = intent.getLongExtra(TransactionAlertNotification.EXTRA_EXPENSE_ID, -1L)

        if (expenseId <= 0) {
            // Nothing can be written; clear the spinner rather than stranding it.
            if (notifIdExtra > 0) TransactionAlertNotification.cancel(context, notifIdExtra)
            return
        }

        val notifId = notifIdExtra.takeIf { it > 0 } ?: expenseId.toInt().coerceAtLeast(1)
        val title = intent.getStringExtra(TransactionAlertNotification.EXTRA_NOTIF_TITLE)
            ?.takeIf { it.isNotBlank() } ?: FALLBACK_TITLE
        val body = intent.getStringExtra(TransactionAlertNotification.EXTRA_NOTIF_BODY)
            ?.takeIf { it.isNotBlank() } ?: FALLBACK_BODY

        val note = NoteReplySanitizer.sanitize(
            RemoteInput.getResultsFromIntent(intent)
                ?.getCharSequence(TransactionAlertNotification.KEY_NOTE_REPLY)
        )

        // The Context handed to a receiver is a ReceiverRestrictedContext — never let it
        // outlive onReceive. goAsync() keeps the process alive for the database write.
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        scope.launch {
            try {
                if (note == null) {
                    TransactionAlertNotification.repostForNoteRetry(
                        appContext, notifId, expenseId, title, body
                    )
                    return@launch
                }

                val rows = runCatching {
                    expenseRepository.updateDescription(expenseId, note)
                }.getOrDefault(0)

                if (rows > 0) {
                    TransactionAlertNotification.postNoteSaved(
                        appContext, notifId, expenseId, title, note
                    )
                } else {
                    TransactionAlertNotification.postNoteFailed(appContext, notifId, title)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val FALLBACK_TITLE = "Expense saved"
        private const val FALLBACK_BODY = "Tap to review"

        /**
         * Outlives the transient receiver instance, which Android discards as soon as
         * onReceive returns. Not GlobalScope, so failures stay contained.
         */
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}
