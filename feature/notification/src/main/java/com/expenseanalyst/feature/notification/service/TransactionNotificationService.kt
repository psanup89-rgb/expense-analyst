package com.expenseanalyst.feature.notification.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.expenseanalyst.feature.notification.parser.ParserRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Listens for incoming notifications and SMS pop-ups.
 * Parsed transactions are forwarded to [PendingNotificationManager]
 * so the UI can prompt the user to confirm before saving.
 *
 * Must be declared in AndroidManifest.xml with:
 *   <service android:name=".TransactionNotificationService"
 *            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
 *     <intent-filter>
 *       <action android:name="android.service.notification.NotificationListenerService" />
 *     </intent-filter>
 *   </service>
 *
 * User must also grant Notification Access in Settings.
 */
@AndroidEntryPoint
class TransactionNotificationService : NotificationListenerService() {

    @Inject lateinit var pendingManager: PendingNotificationManager

    /** Package IDs that typically carry bank/payment notifications. */
    private val financialPackages = setOf(
        "com.google.android.apps.nbu.paisa.user",   // Google Pay
        "com.phonepe.app",                           // PhonePe
        "net.one97.paytm",                           // Paytm
        "com.amazon.mShop.android.shopping",         // Amazon Pay
        "com.hdfc.bank",
        "com.sbi.internet",
        "com.icici.imobile",
        "com.axisbank.retail",
        "com.kotak.mahindra.kotak810",
        "com.sms.messages",                          // Generic SMS
        "com.google.android.apps.messaging",         // Google Messages
        "com.samsung.android.messaging",             // Samsung Messages
        "com.android.mms",                           // AOSP MMS
        "com.oneplus.mms"                            // OnePlus Messages
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val body = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: body

        // Only process notifications from financial apps or containing financial keywords
        val isFinancialSource = financialPackages.any { packageName.contains(it, ignoreCase = true) }
        val hasFinancialKeywords = Regex("""(?i)\b(?:debited|credited|rs\.?|inr|sar|paid|upi|bank)\b""")
            .containsMatchIn(bigText)

        if (!isFinancialSource && !hasFinancialKeywords) return

        // Use the notification title as sender — for SMS notifications this is the SMS sender ID
        // (e.g. "VD-HDFCBK-T"), which bank parsers use to identify themselves. Fall back to
        // package name so app-based notifications (Google Pay, PhonePe) still work.
        val effectiveSender = title.ifBlank { packageName }
        val parsed = ParserRegistry.parse(sender = effectiveSender, body = bigText.ifBlank { body })
            ?: return

        // Only enqueue if amount is plausible (> 0 and < 10 million)
        if (parsed.amount <= 0 || parsed.amount > 10_000_000) return

        pendingManager.enqueue(parsed)
    }
}
