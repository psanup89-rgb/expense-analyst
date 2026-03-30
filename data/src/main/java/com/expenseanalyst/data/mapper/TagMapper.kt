package com.expenseanalyst.data.mapper

import com.expenseanalyst.data.local.entity.TagEntity
import com.expenseanalyst.domain.model.Tag

fun TagEntity.toDomain() = Tag(id = id, name = name)

fun Tag.toEntity() = TagEntity(id = id, name = name)
