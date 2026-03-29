# Skill: Delegated Property Smart Cast (Compose)

## Problem

In Jetpack Compose, ViewModel state is commonly collected via:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

The `by` keyword creates a **delegated property**. Kotlin's smart cast system cannot track nullability through a delegate. This causes compilation errors like:

```
Smart cast to 'String' is impossible, because 'uiState' is a delegated property that might change between checks.
```

For example:
```kotlin
// ❌ WILL NOT COMPILE
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
if (uiState.drillDownTitle != null) {
    ModalBottomSheet(title = uiState.drillDownTitle)  // Error: smart cast impossible
}
```

## Solution

Capture the nullable property to a local `val` **before** the null check:

```kotlin
// ✅ CORRECT
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
val drillDownTitle = uiState.drillDownTitle  // capture to local val
if (drillDownTitle != null) {
    ModalBottomSheet(title = drillDownTitle)  // smart cast works on local val
}
```

## Why This Works

A local `val` is immutable and not delegated — Kotlin can guarantee it won't change between the null check and its use. The Kotlin compiler accepts the smart cast.

## When To Apply

Any time you:
- Use `val x by someFlow.collectAsState()` or `collectAsStateWithLifecycle()`
- Then null-check a field of that state: `if (x.someNullableField != null) { … x.someNullableField … }`

The pattern appears in any screen with conditional UI driven by nullable state (e.g., showing a bottom sheet only when `drillDownTitle != null`).

## Related

- Same issue applies to `val x by remember { mutableStateOf(...) }` if `x` holds a nullable wrapper
- Does NOT apply to `val x = viewModel.uiState.collectAsStateWithLifecycle().value` (not a delegated property) — but that form is less idiomatic in Compose

## Example from codebase

`feature/analytics/src/main/java/com/expenseanalyst/feature/analytics/ui/AnalyticsScreen.kt`:
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
// …
val drillDownTitle = uiState.drillDownTitle
if (drillDownTitle != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissDrillDown,
        sheetState = sheetState,
    ) {
        DrillDownSheet(title = drillDownTitle, …)
    }
}
```
