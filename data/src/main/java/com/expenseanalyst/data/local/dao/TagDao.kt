package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.expenseanalyst.data.local.entity.ExpenseTagCrossRef
import com.expenseanalyst.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/** Row shape for [TagDao.getTagsWithUsage]. */
data class TagUsageRow(
    val id: Long,
    val name: String,
    val expenseCount: Int,
    val ruleCount: Int
)

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT t.* FROM tags t INNER JOIN expense_tags et ON t.id = et.tag_id WHERE et.expense_id = :expenseId")
    fun getTagsForExpense(expenseId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    /** Case-insensitive lookup — see domain/util/TagNames for why tag names ignore case. */
    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE ORDER BY id LIMIT 1")
    suspend fun getTagByNameIgnoreCase(name: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :id")
    fun getTag(id: Long): Flow<TagEntity?>

    /** Expense counts exclude soft-deleted rows, so a tag only on deleted expenses reads unused. */
    @Query(
        """
        SELECT t.id AS id, t.name AS name,
            (SELECT COUNT(*) FROM expense_tags et JOIN expenses e ON e.id = et.expense_id
             WHERE et.tag_id = t.id AND e.is_deleted = 0) AS expenseCount,
            (SELECT COUNT(*) FROM merchant_rule_tags rt WHERE rt.tag_id = t.id) AS ruleCount
        FROM tags t
        ORDER BY t.name COLLATE NOCASE ASC
        """
    )
    fun getTagsWithUsage(): Flow<List<TagUsageRow>>

    @Query("UPDATE tags SET name = :name WHERE id = :id")
    suspend fun renameTag(id: Long, name: String)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query(
        """
        INSERT OR IGNORE INTO expense_tags (expense_id, tag_id)
        SELECT expense_id, :targetId FROM expense_tags WHERE tag_id = :sourceId
        """
    )
    suspend fun copyExpenseLinks(sourceId: Long, targetId: Long)

    @Query(
        """
        INSERT OR IGNORE INTO merchant_rule_tags (rule_id, tag_id)
        SELECT rule_id, :targetId FROM merchant_rule_tags WHERE tag_id = :sourceId
        """
    )
    suspend fun copyRuleLinks(sourceId: Long, targetId: Long)

    /**
     * Copies the source's links onto the target (INSERT OR IGNORE, so links the target already
     * has aren't duplicated — both link tables key on the pair), then deletes the source tag,
     * which cascades away its own links. One transaction, so a crash can't leave both tags
     * holding the links or neither.
     */
    @Transaction
    suspend fun mergeTag(sourceId: Long, targetId: Long) {
        if (sourceId == targetId) return
        copyExpenseLinks(sourceId, targetId)
        copyRuleLinks(sourceId, targetId)
        deleteTag(sourceId)
    }

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTags(query: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenseTagCrossRef(crossRef: ExpenseTagCrossRef)

    @Query("DELETE FROM expense_tags WHERE expense_id = :expenseId")
    suspend fun clearTagsForExpense(expenseId: Long)

    @Transaction
    suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>) {
        clearTagsForExpense(expenseId)
        tagIds.forEach { tagId ->
            insertExpenseTagCrossRef(ExpenseTagCrossRef(expenseId, tagId))
        }
    }
}
