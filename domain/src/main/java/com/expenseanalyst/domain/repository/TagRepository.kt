package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<Tag>>
    fun getTagsForExpense(expenseId: Long): Flow<List<Tag>>
    suspend fun createTag(name: String): Tag
    suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>)
    fun searchTags(query: String): Flow<List<Tag>>
}
