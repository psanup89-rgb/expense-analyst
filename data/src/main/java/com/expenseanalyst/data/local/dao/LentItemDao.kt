package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenseanalyst.data.local.entity.LentItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LentItemDao {

    @Query("SELECT * FROM lent_items WHERE is_deleted = 0 ORDER BY lent_date_millis DESC")
    fun getAllActive(): Flow<List<LentItemEntity>>

    @Query("SELECT * FROM lent_items ORDER BY lent_date_millis DESC")
    fun getAll(): Flow<List<LentItemEntity>>

    @Query("SELECT * FROM lent_items WHERE id = :id")
    suspend fun getById(id: Long): LentItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LentItemEntity): Long

    @Update
    suspend fun update(entity: LentItemEntity)

    @Query("UPDATE lent_items SET is_deleted = 1, updated_at_millis = :nowMillis WHERE id = :id")
    suspend fun softDelete(id: Long, nowMillis: Long)

    @Query("""
        SELECT * FROM lent_items
        WHERE is_deleted = 0
          AND status = 'PENDING'
          AND reminder_datetime_millis IS NOT NULL
          AND reminder_datetime_millis <= :nowMillis
    """)
    suspend fun getDueForReminder(nowMillis: Long): List<LentItemEntity>
}
