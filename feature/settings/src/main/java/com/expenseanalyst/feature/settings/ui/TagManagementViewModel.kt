package com.expenseanalyst.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TagRenameResult
import com.expenseanalyst.domain.model.TagUsage
import com.expenseanalyst.domain.repository.TagRepository
import com.expenseanalyst.domain.util.TagNames
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagManagementViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(TagManagementUiState())

    val uiState: StateFlow<TagManagementUiState> =
        combine(tagRepository.getTagsWithUsage(), _ui) { tags, ui ->
            ui.copy(isLoading = false, tags = tags)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagManagementUiState())

    fun onQueryChange(query: String) = _ui.update { it.copy(query = query) }
    fun dismissDialog() = _ui.update { it.copy(dialog = null) }
    fun clearMessage() = _ui.update { it.copy(message = null) }

    // ── Add ──
    fun showAdd() = _ui.update { it.copy(dialog = TagDialog.Add()) }

    fun onDialogNameChange(name: String) = _ui.update {
        when (val d = it.dialog) {
            is TagDialog.Add -> it.copy(dialog = d.copy(name = name, error = null))
            is TagDialog.Rename -> it.copy(dialog = d.copy(name = name, error = null))
            else -> it
        }
    }

    fun confirmAdd() {
        val dialog = _ui.value.dialog as? TagDialog.Add ?: return
        if (!TagNames.isValid(dialog.name)) {
            _ui.update { it.copy(dialog = dialog.copy(error = "Enter a name")) }
            return
        }
        val existing = uiState.value.tags.firstOrNull { TagNames.sameName(it.tag.name, dialog.name) }
        if (existing != null) {
            _ui.update { it.copy(dialog = dialog.copy(error = "\"${existing.tag.name}\" already exists")) }
            return
        }
        viewModelScope.launch {
            tagRepository.createTag(dialog.name)
            _ui.update { it.copy(dialog = null) }
        }
    }

    // ── Rename ──
    fun showRename(usage: TagUsage) =
        _ui.update { it.copy(dialog = TagDialog.Rename(usage, usage.tag.name)) }

    fun confirmRename() {
        val dialog = _ui.value.dialog as? TagDialog.Rename ?: return
        viewModelScope.launch {
            when (val result = tagRepository.renameTag(dialog.source.tag.id, dialog.name)) {
                TagRenameResult.Renamed -> _ui.update { it.copy(dialog = null) }
                TagRenameResult.InvalidName ->
                    _ui.update { it.copy(dialog = dialog.copy(error = "Enter a name")) }
                is TagRenameResult.NameTaken ->
                    _ui.update { it.copy(dialog = TagDialog.MergeOnRename(dialog.source, result.existing)) }
            }
        }
    }

    // ── Merge ──
    fun showMergePicker(usage: TagUsage) =
        _ui.update { it.copy(dialog = TagDialog.PickMergeTarget(usage)) }

    fun mergeInto(source: TagUsage, target: Tag) {
        viewModelScope.launch {
            tagRepository.mergeTag(source.tag.id, target.id)
            _ui.update {
                it.copy(dialog = null, message = "Merged \"${source.tag.name}\" into \"${target.name}\"")
            }
        }
    }

    // ── Delete ──
    fun showDelete(usage: TagUsage) = _ui.update { it.copy(dialog = TagDialog.ConfirmDelete(usage)) }

    fun deleteCompletely(usage: TagUsage) {
        viewModelScope.launch {
            tagRepository.deleteTag(usage.tag.id)
            _ui.update { it.copy(dialog = null, message = "Deleted \"${usage.tag.name}\"") }
        }
    }
}
