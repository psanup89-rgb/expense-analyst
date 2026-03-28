## skill: viewmodel-confirm-dialog
agent: FeatureAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [compose, viewmodel, uistate, dialog, confirmation, ux]

# Confirmation Dialog Pattern (Request / Confirm / Cancel)

## When to use this

When a destructive or irreversible action in a Composable screen needs user confirmation before executing (delete, dismiss, clear all, unlink). The naive approach of calling the action directly from an `onClick` lambda works but is not testable and leaks UI logic.

This pattern keeps the "am I confirming?" state in the ViewModel/UiState (where it belongs) and keeps the Composable purely reactive.

## What to do

**Step 1 — Add state to UiState**

For a single-item confirmation (e.g. delete by ID):
```kotlin
data class MyUiState(
    // ...
    val pendingDeleteId: Long? = null   // null = no dialog; non-null = dialog showing for this ID
)
```

For a boolean confirmation (e.g. clear all):
```kotlin
data class MyUiState(
    // ...
    val showClearAllConfirm: Boolean = false
)
```

**Step 2 — Add request / confirm / cancel to ViewModel**

```kotlin
// Single item pattern
fun requestDelete(id: Long) = _ui.update { it.copy(pendingDeleteId = id) }
fun cancelDelete() = _ui.update { it.copy(pendingDeleteId = null) }
fun confirmDelete() {
    val id = _ui.value.pendingDeleteId ?: return
    _ui.update { it.copy(pendingDeleteId = null) }   // dismiss dialog first
    viewModelScope.launch { repository.delete(id) }
}

// Boolean pattern
fun requestClearAll() = _ui.update { it.copy(showClearAllConfirm = true) }
fun cancelClearAll() = _ui.update { it.copy(showClearAllConfirm = false) }
fun confirmClearAll() {
    _ui.update { it.copy(showClearAllConfirm = false) }
    viewModelScope.launch { repository.deleteAll() }
}
```

**Step 3 — Render the dialog in the Composable, BEFORE the Scaffold**

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// Dialogs go BEFORE Scaffold to avoid z-index / inset issues
if (uiState.pendingDeleteId != null) {
    AlertDialog(
        onDismissRequest = viewModel::cancelDelete,
        title = { Text("Delete item?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = viewModel::confirmDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
        }
    )
}

Scaffold(...) { ... }
```

**Step 4 — Replace the original direct call with `request*()`**

```kotlin
// Before
Button(onClick = { viewModel.delete(item.id) }) { Text("Delete") }

// After
Button(onClick = { viewModel.requestDelete(item.id) }) { Text("Delete") }
```

## Example

`PendingInboxViewModel` (commit `710300f`):
- `pendingDismissId: Long?` for single-item dismiss
- `showDismissAllConfirm: Boolean` for Clear All
- `requestDismiss()` / `confirmDismiss()` / `cancelDismiss()`
- `requestDismissAll()` / `confirmDismissAll()` / `cancelDismissAll()`

Dialog message for inbox dismiss:
> "This transaction has not been added to your expenses yet. Are you sure you want to dismiss it?"

Dialog message for Clear All (includes count):
> "None of these transactions have been added to your expenses yet. Clearing will permanently remove all N pending items."

## Pitfalls

- **Placing `AlertDialog` inside `Scaffold { }` content lambda** — causes the dialog to sit below the scaffold's surface layer, appearing behind the bottom nav on some devices. Always render dialogs outside and before the `Scaffold`.
- **Calling the action before dismissing the dialog** — if the repository operation is fast, the state update from the delete can arrive before the dialog close state, causing a brief flicker. Always update `_ui` to close the dialog before launching the coroutine.
- **Using `var showDialog by remember { mutableStateOf(false) }` in the Composable** — this is not testable and resets on recomposition. ViewModel state is the right home for this.

## Related skills

- `viewmodel-bottom-sheet.md` (same request/confirm pattern for sheets)
