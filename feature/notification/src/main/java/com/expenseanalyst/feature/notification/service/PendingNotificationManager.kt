package com.expenseanalyst.feature.notification.service

import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds parsed transactions that are awaiting user confirmation.
 * The UI observes [pending] and shows a confirmation banner.
 */
@Singleton
class PendingNotificationManager @Inject constructor() {

    private val _pending = MutableStateFlow<ParsedTransaction?>(null)
    val pending: StateFlow<ParsedTransaction?> = _pending.asStateFlow()

    fun enqueue(transaction: ParsedTransaction) {
        _pending.value = transaction
    }

    fun consume(): ParsedTransaction? {
        val tx = _pending.value
        _pending.value = null
        return tx
    }

    fun dismiss() {
        _pending.value = null
    }
}
