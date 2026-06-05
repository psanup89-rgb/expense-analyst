package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.LentItemDao
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.data.mapper.toEntity
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.repository.LentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LentRepositoryImpl @Inject constructor(
    private val dao: LentItemDao
) : LentRepository {

    override fun getLentItems(includeDeleted: Boolean): Flow<List<LentItem>> =
        if (includeDeleted) dao.getAll().map { list -> list.map { it.toDomain() } }
        else dao.getAllActive().map { list -> list.map { it.toDomain() } }

    override suspend fun getLentItemById(id: Long): LentItem? =
        dao.getById(id)?.toDomain()

    override suspend fun addLentItem(item: LentItem): Long {
        val now = System.currentTimeMillis()
        return dao.insert(item.toEntity(createdAt = now, updatedAt = now))
    }

    override suspend fun updateLentItem(item: LentItem) {
        val existing = dao.getById(item.id)
        val createdAt = existing?.createdAtMillis ?: System.currentTimeMillis()
        dao.update(item.toEntity(createdAt = createdAt, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun softDeleteLentItem(id: Long) {
        dao.softDelete(id, System.currentTimeMillis())
    }

    override suspend fun getLentItemsDueForReminder(nowMillis: Long): List<LentItem> =
        dao.getDueForReminder(nowMillis).map { it.toDomain() }
}
