package com.expenseanalyst.feature.notification.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.expenseanalyst.feature.notification.BuildConfig
import com.expenseanalyst.feature.notification.parser.ParserRegistry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Debug-only receiver that simulates an incoming bank SMS via adb.
 *
 * Usage:
 *   adb shell am broadcast -a com.expenseanalyst.TEST_SMS \
 *     --es sender "ALRAJHI" \
 *     --es body "Your account has been debited SAR 150.00 At:Noon Ref:12345 Bal:SAR 5000.00"
 *
 * Silently does nothing in release builds.
 */
@AndroidEntryPoint
class SmsTestReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pendingManager: PendingNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return

        val sender = intent.getStringExtra("sender") ?: return
        val body = intent.getStringExtra("body") ?: return

        val parsed = ParserRegistry.parse(sender = sender, body = body)?.copy(rawBody = body) ?: return
        if (parsed.amount <= 0 || parsed.amount > 10_000_000) return

        pendingManager.enqueue(parsed)
    }
}
