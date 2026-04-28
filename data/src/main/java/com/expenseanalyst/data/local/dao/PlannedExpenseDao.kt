package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenseanalyst.data.local.entity.PlannedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedExpenseDao {

    @Query("SELECT * FROM planned_expenses WHERE month = :month AND year = :year AND is_deleted = 0 ORDER BY created_at_millis ASC")
    fun getByMonthYear(month: Int, year: Int): Flow<List<PlannedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlannedExpenseEntity): Long

    @Update
    suspend fun update(item: PlannedExpenseEntity)

    @Query("UPDATE planned_expenses SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM planned_expenses WHERE month = :month AND year = :year AND is_deleted = 0")
    suspend fun getByMonthYearSnapshot(month: Int, year: Int): List<PlannedExpenseEntity>
}
