package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenseanalyst.data.local.entity.PendingNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingNotificationDao {

    @Query("SELECT * FROM pending_notifications ORDER BY detected_at_millis DESC")
    fun getAll(): Flow<List<PendingNotificationEntity>>

    @Query("SELECT COUNT(*) FROM pending_notifications")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM pending_notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PendingNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingNotificationEntity): Long

    @Query("DELETE FROM pending_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Find a recent pending notification whose raw_body matches the given text.
     * Used for live notification dedup — prevents the same SMS from being enqueued twice
     * (e.g. dual-SIM retry, notification replay).
     */
    @Query(
        """SELECT * FROM pending_notifications
           WHERE raw_body = :rawBody
             AND detected_at_millis >= :sinceMillis
           LIMIT 1"""
    )
    suspend fun findRecentByRawBody(rawBody: String, sinceMillis: Long): PendingNotificationEntity?

    /**
     * Find a queued BILL for the same biller and amount within [sinceMillis].
     * Billers re-send the same statement as a reminder every few days with only the date
     * wording changed, so [findRecentByRawBody] (exact-body match) never catches those.
     */
    @Query(
        """SELECT * FROM pending_notifications
           WHERE pending_type = 'BILL'
             AND biller_name = :billerName
             AND ABS(amount - :amount) < 0.01
             AND detected_at_millis >= :sinceMillis
           LIMIT 1"""
    )
    suspend fun findRecentBillByBillerAndAmount(
        billerName: String,
        amount: Double,
        sinceMillis: Long
    ): PendingNotificationEntity?

    @Query(
        """SELECT * FROM pending_notifications
           WHERE pending_type = 'BILL_REMINDER' AND linked_bill_id = :billId
           LIMIT 1"""
    )
    suspend fun findReminderCardForBill(billId: Long): PendingNotificationEntity?

    // detected_at is left alone on purpose: bumping it would slide the 35-day reminder window
    // forward forever, and next month's bill for the same amount would be swallowed as a reminder.
    @Query("UPDATE pending_notifications SET reminder_count = reminder_count + 1 WHERE id = :id")
    suspend fun addReminder(id: Long)

    @Query("DELETE FROM pending_notifications")
    suspend fun deleteAll()
}
