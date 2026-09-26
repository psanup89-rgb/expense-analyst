package com.expenseanalyst.feature.settings.ui

import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TagUsage

data class TagManagementUiState(
    val isLoading: Boolean = true,
    val tags: List<TagUsage> = emptyList(),
    val query: String = "",
    val dialog: TagDialog? = null,
    val message: String? = null
) {
    val visibleTags: List<TagUsage> =
        if (query.isBlank()) tags else tags.filter { it.tag.name.contains(query.trim(), ignoreCase = true) }
}

/** The one dialog (if any) open on the Manage Tags screen. */
sealed interface TagDialog {
    data class Add(val name: String = "", val error: String? = null) : TagDialog
    data class Rename(val source: TagUsage, val name: String, val error: String? = null) : TagDialog

    /** Renaming collided with [target]'s name; offer to merge instead. */
    data class MergeOnRename(val source: TagUsage, val target: Tag) : TagDialog

    /** Choose which tag [source]'s expenses and rules move to. Used by "Merge into…" and by delete's "Move". */
    data class PickMergeTarget(val source: TagUsage) : TagDialog

    /** Delete: either move the links to another tag, or remove the tag completely. */
    data class ConfirmDelete(val source: TagUsage) : TagDialog
}
