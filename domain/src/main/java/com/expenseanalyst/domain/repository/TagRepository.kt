package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TagRenameResult
import com.expenseanalyst.domain.model.TagUsage
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    fun getTagsForExpense(expenseId: Long): Flow<List<Tag>>
    /**
     * Creates a tag, or returns the existing one when a tag with the same name already exists
     * **ignoring case** (see [com.expenseanalyst.domain.util.TagNames]).
     */
    suspend fun createTag(name: String): Tag
    suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>)
    fun searchTags(query: String): Flow<List<Tag>>

    /** Every tag with its expense and rule usage counts, ordered by name. */
    fun getTagsWithUsage(): Flow<List<TagUsage>>

    fun getTag(id: Long): Flow<Tag?>

    suspend fun renameTag(id: Long, newName: String): TagRenameResult

    /**
     * Moves every expense and rule link from [sourceId] onto [targetId], then deletes the source
     * tag. Links the target already has are not duplicated. Atomic.
     */
    suspend fun mergeTag(sourceId: Long, targetId: Long)

    /**
     * Deletes the tag. Its links are removed from every expense and rule (the expenses and rules
     * themselves are untouched). Tags have no soft-delete flag; a deleted tag is gone.
     */
    suspend fun deleteTag(id: Long)
}
