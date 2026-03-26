package com.expenseanalyst.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean,
    val sortOrder: Int
)
