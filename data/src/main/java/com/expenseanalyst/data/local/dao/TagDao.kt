package com.expenseanalyst.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.expenseanalyst.data.local.entity.ExpenseTagCrossRef
import com.expenseanalyst.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

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
