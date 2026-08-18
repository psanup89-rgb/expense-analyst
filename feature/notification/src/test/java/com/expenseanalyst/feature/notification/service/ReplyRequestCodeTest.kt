package com.expenseanalyst.feature.notification.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The content PendingIntent uses notifId itself as its request code, so the reply request code
 * must never coincide with any notifId. A collision would deliver a typed note to the wrong
 * expense, so this arithmetic is pinned down explicitly.
 */
class ReplyRequestCodeTest {

    private val notifIds = listOf(1, 2, 3, 2000, 12_345, Int.MAX_VALUE - 1, Int.MAX_VALUE)

    @Test
    fun `reply codes are always negative so they cannot collide with a notif id`() {
        notifIds.forEach { id ->
            val code = TransactionAlertNotification.replyRequestCode(id)
            assertTrue(code < 0, "replyRequestCode($id) = $code should be negative")
        }
    }

    @Test
    fun `reply code never equals its own notif id`() {
        notifIds.forEach { id ->
            assertNotEquals(id, TransactionAlertNotification.replyRequestCode(id))
        }
    }

    @Test
    fun `distinct notif ids produce distinct reply codes`() {
        val codes = notifIds.map { TransactionAlertNotification.replyRequestCode(it) }
        assertEquals(notifIds.size, codes.toSet().size, "reply codes must be injective")
    }

    @Test
    fun `mapping is reversible`() {
        notifIds.forEach { id ->
            val code = TransactionAlertNotification.replyRequestCode(id)
            assertEquals(id, TransactionAlertNotification.replyRequestCode(code))
        }
    }
}
