package com.expenseanalyst.domain.model

/**
 * A tag with how widely it is used, for the Manage Tags screen. [expenseCount] excludes
 * soft-deleted expenses, so a tag only on deleted rows reads as unused.
 */
data class TagUsage(
    val tag: Tag,
    val expenseCount: Int,
    val ruleCount: Int
)

/** Outcome of renaming a tag. */
sealed interface TagRenameResult {
    data object Renamed : TagRenameResult
    data object InvalidName : TagRenameResult

    /**
     * Another tag already has this name (ignoring case). Renaming would collide on the unique
     * index, so the caller is expected to offer a merge into [existing] instead.
     */
    data class NameTaken(val existing: Tag) : TagRenameResult
}
