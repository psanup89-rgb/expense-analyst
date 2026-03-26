package com.expenseanalyst.data.mapper

import com.expenseanalyst.data.local.entity.CategoryEntity
import com.expenseanalyst.domain.model.Category

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault,
    sortOrder = sortOrder
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault,
    sortOrder = sortOrder
)
