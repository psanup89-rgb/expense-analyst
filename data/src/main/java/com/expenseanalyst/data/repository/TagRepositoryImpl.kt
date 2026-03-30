package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.TagDao
import com.expenseanalyst.data.local.entity.TagEntity
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {

    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { entities -> entities.map { it.toDomain() } }

    override fun getTagsForExpense(expenseId: Long): Flow<List<Tag>> =
        tagDao.getTagsForExpense(expenseId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createTag(name: String): Tag {
        val trimmed = name.trim()
        val insertedId = tagDao.insertTag(TagEntity(name = trimmed))
        return if (insertedId != -1L) {
            Tag(id = insertedId, name = trimmed)
        } else {
            // Already exists — fetch existing
            val existing = tagDao.getTagByName(trimmed)!!
            existing.toDomain()
        }
    }

    override suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>) {
        tagDao.setTagsForExpense(expenseId, tagIds)
    }

    override fun searchTags(query: String): Flow<List<Tag>> =
        tagDao.searchTags(query).map { entities -> entities.map { it.toDomain() } }
}
