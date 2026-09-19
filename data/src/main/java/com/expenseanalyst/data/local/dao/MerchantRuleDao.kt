package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.expenseanalyst.data.local.entity.MerchantRuleEntity
import com.expenseanalyst.data.local.entity.MerchantRuleTagCrossRef
import com.expenseanalyst.data.local.relation.MerchantRuleWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Query("SELECT * FROM merchant_rules ORDER BY created_at_utc_millis DESC")
    fun getAll(): Flow<List<MerchantRuleEntity>>

    @Transaction
    @Query("SELECT * FROM merchant_rules ORDER BY created_at_utc_millis DESC")
    fun getAllWithTags(): Flow<List<MerchantRuleWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRuleEntity): Long

    @Query("DELETE FROM merchant_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM merchant_rules WHERE merchant_pattern = :pattern LIMIT 1")
    suspend fun findByPattern(pattern: String): MerchantRuleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: MerchantRuleTagCrossRef)

    @Query("DELETE FROM merchant_rule_tags WHERE rule_id = :ruleId")
    suspend fun clearTagsForRule(ruleId: Long)

    @Transaction
    suspend fun setTagsForRule(ruleId: Long, tagIds: List<Long>) {
        clearTagsForRule(ruleId)
        tagIds.forEach { tagId -> insertCrossRef(MerchantRuleTagCrossRef(ruleId, tagId)) }
    }
}
