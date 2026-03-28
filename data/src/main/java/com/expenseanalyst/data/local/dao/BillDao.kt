package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenseanalyst.data.local.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Query("SELECT * FROM bills WHERE is_deleted = 0 ORDER BY created_at_millis DESC")
    fun getAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE is_deleted = 0 AND status = :status ORDER BY created_at_millis DESC")
    fun getByStatus(status: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<BillEntity?>

    @Query("""
        SELECT * FROM bills
        WHERE is_deleted = 0
          AND status IN ('PENDING', 'PARTIAL')
          AND biller_name = :billerName
          AND (account_id = :accountId OR :accountId IS NULL OR account_id IS NULL)
        ORDER BY created_at_millis DESC
        LIMIT 1
    """)
    suspend fun findOpenByBiller(billerName: String, accountId: Long?): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Query("UPDATE bills SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
}
