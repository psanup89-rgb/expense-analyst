package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenseanalyst.data.local.entity.TransferRecipientRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferRecipientRuleDao {

    @Query("SELECT * FROM transfer_recipient_rules ORDER BY created_at_utc_millis DESC")
    fun getAll(): Flow<List<TransferRecipientRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: TransferRecipientRuleEntity): Long

    @Query("DELETE FROM transfer_recipient_rules WHERE recipient_key = :key")
    suspend fun deleteByKey(key: String)
}
