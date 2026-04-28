package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenseanalyst.data.local.entity.SalaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryDao {

    @Query("SELECT * FROM salary_entries WHERE month = :month AND year = :year LIMIT 1")
    fun getByMonthYear(month: Int, year: Int): Flow<SalaryEntryEntity?>

    @Query("SELECT * FROM salary_entries ORDER BY year DESC, month DESC")
    fun getAll(): Flow<List<SalaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SalaryEntryEntity): Long

    @Query("DELETE FROM salary_entries WHERE month = :month AND year = :year")
    suspend fun delete(month: Int, year: Int)
}
