package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenseanalyst.data.local.entity.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Query("SELECT * FROM merchant_rules ORDER BY created_at_utc_millis DESC")
    fun getAll(): Flow<List<MerchantRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRuleEntity): Long

    @Query("DELETE FROM merchant_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM merchant_rules WHERE merchant_pattern = :pattern LIMIT 1")
    suspend fun findByPattern(pattern: String): MerchantRuleEntity?
}
