package com.expenseanalyst.data.local.dao

import androidx.room.*
import com.expenseanalyst.data.local.entity.EmiGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmiGroupDao {

    @Query("SELECT * FROM emi_groups ORDER BY start_date_utc_millis DESC")
    fun getAllEmiGroups(): Flow<List<EmiGroupEntity>>

    @Query("SELECT * FROM emi_groups WHERE id = :id")
    fun getEmiGroupById(id: Long): Flow<EmiGroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmiGroup(emiGroup: EmiGroupEntity): Long

    @Update
    suspend fun updateEmiGroup(emiGroup: EmiGroupEntity)
}
