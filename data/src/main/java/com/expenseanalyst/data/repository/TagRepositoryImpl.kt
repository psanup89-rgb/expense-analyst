package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.TagDao
import com.expenseanalyst.data.local.entity.TagEntity
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TagRenameResult
import com.expenseanalyst.domain.model.TagUsage
import com.expenseanalyst.domain.util.TagNames
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
        val normalized = TagNames.normalize(name)
        // Reuse an existing tag that differs only in case. The unique index is case-sensitive,
        // so without this "subscription" would become a second tag beside "Subscription".
        tagDao.getTagByNameIgnoreCase(normalized)?.let { return it.toDomain() }
        val insertedId = tagDao.insertTag(TagEntity(name = normalized))
        return if (insertedId != -1L) {
            Tag(id = insertedId, name = normalized)
        } else {
            tagDao.getTagByNameIgnoreCase(normalized)!!.toDomain()
        }
    }

    override suspend fun setTagsForExpense(expenseId: Long, tagIds: List<Long>) {
        tagDao.setTagsForExpense(expenseId, tagIds)
    }

    override fun searchTags(query: String): Flow<List<Tag>> =
        tagDao.searchTags(query).map { entities -> entities.map { it.toDomain() } }

    override fun getTagsWithUsage(): Flow<List<TagUsage>> =
        tagDao.getTagsWithUsage().map { rows ->
            rows.map { TagUsage(Tag(it.id, it.name), it.expenseCount, it.ruleCount) }
        }

    override fun getTag(id: Long): Flow<Tag?> = tagDao.getTag(id).map { it?.toDomain() }

    override suspend fun renameTag(id: Long, newName: String): TagRenameResult {
        if (!TagNames.isValid(newName)) return TagRenameResult.InvalidName
        val normalized = TagNames.normalize(newName)
        val clash = tagDao.getTagByNameIgnoreCase(normalized)
        // A clash with itself is fine: that's a case-only rename like "haven" -> "Haven".
        if (clash != null && clash.id != id) return TagRenameResult.NameTaken(clash.toDomain())
        tagDao.renameTag(id, normalized)
        return TagRenameResult.Renamed
    }

    override suspend fun mergeTag(sourceId: Long, targetId: Long) = tagDao.mergeTag(sourceId, targetId)

    override suspend fun deleteTag(id: Long) = tagDao.deleteTag(id)
}
