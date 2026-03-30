---
skill: compose-experimental-in-nested-lambda
agent: FeatureAgent
created: 2026-03-30
last_used: 2026-03-30
tags: [compose, experimental, opt-in, kotlin, flowrow, lambda]

# Compose Experimental API in Nested Content Lambdas

## When to use this

When using a Compose experimental API (e.g., `FlowRow` which requires `@ExperimentalLayoutApi`) inside a content lambda passed to another composable (e.g., a `Column { }`, `Card { }`, or `LazyColumn` item), even if the outer function is annotated with `@OptIn`. The Compose compiler sometimes raises the experimental error at the lambda call site rather than the function scope.

## What to do

**Do NOT** rely on `@OptIn` on the outer composable function propagating through nested lambdas:
```kotlin
// This may still fail at the FlowRow call site:
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyScreen(...) {
    Column {
        FlowRow { ... }  // ← compiler may still error here
    }
}
```

**DO** extract the experimental call into its own private composable function with a direct `@OptIn`:
```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsDetailRow(tags: List<Tag>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Tags", ...)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag -> FilterChip(...) }
        }
    }
}
```

Then call the helper from the outer composable without any opt-in needed at the call site:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(...) {
    Column {
        TagsDetailRow(expense.tags)  // ← no opt-in needed here
    }
}
```

## Example

**Failing approach** (`ExpenseDetailScreen.kt`):
```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseDetailScreen(...) {
    // ...nested inside Card > Column > verticalScroll > Column:
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag -> FilterChip(selected = false, onClick = {}, label = { Text(tag.name) }) }
    }
    // Error: "The API of this layout is experimental and is likely to change in the future."
}
```

**Working approach**:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(...) {
    if (expense.tags.isNotEmpty()) {
        TagsDetailRow(expense.tags)  // ← clean, no error
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsDetailRow(tags: List<Tag>) {
    Column(...) {
        FlowRow(...) { tags.forEach { FilterChip(...) } }
    }
}
```

## Pitfalls

- The error message "The API of this layout is experimental and is likely to change in the future." is a **compiler error**, not a lint warning. It fails the build.
- This doesn't always happen — shallow lambdas often work fine. It's specifically deep nesting (composable → content lambda → content lambda → experimental call) that triggers it.
- Don't add `@OptIn` to `build.gradle.kts` as a compiler flag workaround — it suppresses the error globally and makes future experimental API usage invisible.

## Related skills

- None
