package com.expenseanalyst.feature.notification.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.expenseanalyst.feature.notification.parser.ParserRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives incoming SMS messages directly from the system.
 *
 * This is the PRIMARY path for detecting bank transaction SMS alerts.
 * It works even when the app is killed and doesn't require the user to
 * manually enable Notification Access in system settings.
 *
 * Requires: READ_SMS + RECEIVE_SMS permissions in the manifest.
 *
 * The NotificationListenerService remains as a secondary path that also
 * catches push notifications from banking apps (Google Pay, PhonePe, etc.).
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pendingManager: PendingNotificationManager

    private val financialKeywords = Regex(
        """(?i)\b(?:debited|credited|debit|credit|rs\.?|inr|sar|paid|upi|bank|purchase|balance|card|amount|transaction|withdrawn|transferred)\b"""
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Stitch multi-part SMS back together (PDU parts arrive as separate SmsMessage objects)
        val sender = messages.first().originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }

        if (body.isBlank() || !financialKeywords.containsMatchIn(body)) return

        val parsed = ParserRegistry.parse(sender = sender, body = body)?.copy(rawBody = body) ?: return
        if (parsed.amount <= 0 || parsed.amount > 10_000_000) return

        pendingManager.enqueue(parsed)
    }
}
