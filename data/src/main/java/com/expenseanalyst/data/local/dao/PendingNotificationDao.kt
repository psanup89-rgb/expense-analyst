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

    @Query("DELETE FROM pending_notifications")
    suspend fun deleteAll()
}
