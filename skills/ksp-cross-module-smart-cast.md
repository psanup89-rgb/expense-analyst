## skill: ksp-cross-module-smart-cast
agent: DataAgent, FeatureAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [kotlin, ksp, hilt, compilation, smart-cast, multi-module]

# KSP Compilation Error: Smart Cast on Cross-Module Nullable Properties

## When to use this

When you get a Kotlin compilation error of the form:

```
e: Smart cast to 'Double' is impossible, because 'totalDue' is a public API property declared in different module.
e: Smart cast to 'Long' is impossible, because 'dueDateMillis' is a public API property declared in different module.
```

This happens when you read a nullable property from a domain model (`val foo: Double? = null`) inside a `feature/*` module and try to use it after a null check in the same expression. The Kotlin compiler cannot guarantee thread safety for public API properties from other modules.

## What to do

Extract the property to a local `val` before the null check. Kotlin CAN smart-cast local vals.

**Step 1**: Find the failing line. It will look like:
```kotlin
if (openBill.totalDue == null || paid >= openBill.totalDue) { ... }
//                                              ^^^^^^^^^^^ FAILS
```

**Step 2**: Extract to a local variable immediately before the check:
```kotlin
val billTotalDue = openBill.totalDue   // local val — smart cast works
if (billTotalDue == null || paid >= billTotalDue) { ... }
```

**Step 3**: Apply the same fix for every nullable property from a different module used in a conditional expression.

Common trigger sites in this project:
- `bill.totalDue` compared in `AddExpenseViewModel.saveExpense()`
- `bill.dueDateMillis` used in `BillsScreen` `BillCard` composable
- Any domain model property accessed in `feature/*` after a null check

## Example

**Before (fails to compile):**
```kotlin
val newStatus = if (openBill.totalDue == null || paid >= openBill.totalDue) {
    BillStatus.SETTLED
} else {
    BillStatus.PARTIAL
}
```

**After (compiles):**
```kotlin
val billTotalDue = openBill.totalDue
val newStatus = if (billTotalDue == null || paid >= billTotalDue) {
    BillStatus.SETTLED
} else {
    BillStatus.PARTIAL
}
```

**In a Composable (BillsScreen):**
```kotlin
val dueDateMillis = bill.dueDateMillis          // extract here
val isOverdue = dueDateMillis != null && dueDateMillis < now
// ...
if (dueDateMillis != null) {
    val dateStr = SimpleDateFormat(...).format(Date(dueDateMillis))
}
```

## Pitfalls

- **Do not add `!!`** — that crashes at runtime if null. The local val pattern is the right fix.
- **KSP error message is misleading** — it says "InjectProcessingStep was unable to process" which sounds like a Hilt/DI error, but the root cause is this Kotlin smart cast issue. Look for the actual `e: Smart cast` error lines buried above the KSP errors.
- **The KSP error cascades** — when one class fails to compile, Hilt cannot process the whole module and ALL bindings fail. One smart cast fix can unblock 10+ KSP errors.

## Related skills

- `build-full-clean.md`
