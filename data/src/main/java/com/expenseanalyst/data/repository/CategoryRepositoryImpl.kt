package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.CategoryDao
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.data.mapper.toEntity
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override fun getCategoryById(id: Long): Flow<Category?> =
        categoryDao.getCategoryById(id).map { it?.toDomain() }

    override suspend fun addCategory(category: Category): Long =
        categoryDao.insertCategory(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category.toEntity())

    override suspend fun updateSortOrder(categories: List<Category>) =
        categoryDao.updateAll(categories.map { it.toEntity() })

    override suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category.toEntity())
}
