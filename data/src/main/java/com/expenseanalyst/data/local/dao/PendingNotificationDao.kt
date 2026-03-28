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

    @Query("DELETE FROM pending_notifications")
    suspend fun deleteAll()
}
