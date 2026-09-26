package com.expenseanalyst.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.domain.model.TagUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    onBack: () -> Unit,
    onTagClick: (Long) -> Unit,
    viewModel: TagManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    uiState.dialog?.let { dialog ->
        TagDialogHost(
            dialog = dialog,
            allTags = uiState.tags,
            onNameChange = viewModel::onDialogNameChange,
            onConfirmAdd = viewModel::confirmAdd,
            onConfirmRename = viewModel::confirmRename,
            onPickMergeTarget = viewModel::showMergePicker,
            onMergeInto = viewModel::mergeInto,
            onDeleteCompletely = viewModel::deleteCompletely,
            onDismiss = viewModel::dismissDialog
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Tags", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, contentDescription = "Add tag") }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search tags…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (uiState.visibleTags.isEmpty()) {
                item {
                    Text(
                        text = if (uiState.tags.isEmpty()) "No tags yet. Tap + to add one." else "No tags match.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(uiState.visibleTags, key = { it.tag.id }) { usage ->
                TagRow(
                    usage = usage,
                    onClick = { onTagClick(usage.tag.id) },
                    onRename = { viewModel.showRename(usage) },
                    onMerge = { viewModel.showMergePicker(usage) },
                    onDelete = { viewModel.showDelete(usage) }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

/** What happens to [usage]'s links when it is merged into [targetName], as a sentence. */
internal fun mergeConsequence(usage: TagUsage, targetName: String): String =
    if (usage.expenseCount == 0 && usage.ruleCount == 0) {
        "\"${usage.tag.name}\" isn't used anywhere, so it's simply removed."
    } else {
        "Its ${usageLabel(usage).lowercase()} move to \"$targetName\", and \"${usage.tag.name}\" is removed."
    }

/** "3 expenses · 1 rule", or "Unused". Shared with the dialogs so the counts read the same. */
internal fun usageLabel(usage: TagUsage): String {
    if (usage.expenseCount == 0 && usage.ruleCount == 0) return "Unused"
    val e = "${usage.expenseCount} expense${if (usage.expenseCount == 1) "" else "s"}"
    val r = "${usage.ruleCount} rule${if (usage.ruleCount == 1) "" else "s"}"
    return "$e · $r"
}

@Composable
private fun TagRow(
    usage: TagUsage,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(usage.tag.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    usageLabel(usage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options for ${usage.tag.name}")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename() })
                    DropdownMenuItem(text = { Text("Merge into…") }, onClick = { menuOpen = false; onMerge() })
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color(0xFFFF5555)) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagDialogHost(
    dialog: TagDialog,
    allTags: List<TagUsage>,
    onNameChange: (String) -> Unit,
    onConfirmAdd: () -> Unit,
    onConfirmRename: () -> Unit,
    onPickMergeTarget: (TagUsage) -> Unit,
    onMergeInto: (TagUsage, com.expenseanalyst.domain.model.Tag) -> Unit,
    onDeleteCompletely: (TagUsage) -> Unit,
    onDismiss: () -> Unit
) {
    when (dialog) {
        is TagDialog.Add -> NameDialog(
            title = "New tag",
            name = dialog.name,
            error = dialog.error,
            confirmLabel = "Add",
            onNameChange = onNameChange,
            onConfirm = onConfirmAdd,
            onDismiss = onDismiss
        )

        is TagDialog.Rename -> NameDialog(
            title = "Rename \"${dialog.source.tag.name}\"",
            name = dialog.name,
            error = dialog.error,
            confirmLabel = "Rename",
            onNameChange = onNameChange,
            onConfirm = onConfirmRename,
            onDismiss = onDismiss
        )

        is TagDialog.MergeOnRename -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("\"${dialog.target.name}\" already exists") },
            text = {
                Text(
                    "Merge \"${dialog.source.tag.name}\" into \"${dialog.target.name}\"? " +
                        mergeConsequence(dialog.source, dialog.target.name)
                )
            },
            confirmButton = { TextButton(onClick = { onMergeInto(dialog.source, dialog.target) }) { Text("Merge") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )

        is TagDialog.PickMergeTarget -> {
            val targets = allTags.filter { it.tag.id != dialog.source.tag.id }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Move \"${dialog.source.tag.name}\" to…") },
                text = {
                    Column {
                        Text(
                            mergeConsequence(dialog.source, "the tag you pick"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        if (targets.isEmpty()) {
                            Text("There are no other tags to move it to.")
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                                items(targets, key = { it.tag.id }) { target ->
                                    ListItem(
                                        headlineContent = { Text(target.tag.name) },
                                        supportingContent = { Text(usageLabel(target)) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable { onMergeInto(dialog.source, target.tag) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }

        is TagDialog.ConfirmDelete -> {
            val usage = dialog.source
            val inUse = usage.expenseCount > 0 || usage.ruleCount > 0
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Delete \"${usage.tag.name}\"?") },
                text = {
                    Text(
                        if (inUse) {
                            "Used on ${usageLabel(usage).lowercase()}. You can move those to another tag, " +
                                "or remove the tag from all of them. The expenses themselves are kept."
                        } else {
                            "This tag isn't used anywhere."
                        }
                    )
                },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        if (inUse && allTags.size > 1) {
                            TextButton(onClick = { onPickMergeTarget(usage) }) { Text("Move to another tag") }
                        }
                        TextButton(
                            onClick = { onDeleteCompletely(usage) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                        ) { Text(if (inUse) "Remove completely" else "Delete") }
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    name: String,
    error: String?,
    confirmLabel: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                placeholder = { Text("Tag name") }
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
