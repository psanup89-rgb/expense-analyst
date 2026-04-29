# kotlinx-datetime Import Gotchas

When using `kotlinx-datetime` in feature modules, certain extension functions require explicit imports — they are NOT auto-resolved by the IDE or compiler.

## Common Unresolved Reference Errors

### `atStartOfDayIn` on `LocalDate`
```kotlin
// WRONG — will fail with "Unresolved reference 'atStartOfDayIn'"
val millis = kotlinx.datetime.LocalDate(2026, 4, 1)
    .atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

// RIGHT — explicit imports required
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

val millis = LocalDate(2026, 4, 1)
    .atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
```

### `toLocalDateTime` on `Instant`
```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
```

### `Instant.fromEpochMilliseconds`
```kotlin
import kotlinx.datetime.Instant
// This is a companion function, not an extension — but still needs the Instant import
val instant = Instant.fromEpochMilliseconds(millis)
```

## Dependency Requirement

Any `:feature` module that uses `kotlinx-datetime` types directly (e.g. `Expense.date` which is `Instant`) needs the dependency explicitly — transitivity from `:domain` is NOT sufficient:

```kotlin
// feature/*/build.gradle.kts
implementation(libs.kotlinx.datetime)
```

## Month Range Pattern

Common pattern for getting start/end millis for a given month (used in Budget, Analytics):

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

private fun monthRange(month: Int, year: Int): Pair<Long, Long> {
    val tz = TimeZone.currentSystemDefault()
    val start = LocalDate(year, month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
    val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    val end = nextMonth.atStartOfDayIn(tz).toEpochMilliseconds()
    return start to end
}
```

When passing these millis to `ExpenseRepository.getExpensesByDateRange()`, convert to `Instant` first — that interface takes `Instant`, not `Long`:

```kotlin
import kotlinx.datetime.Instant

expenseRepository.getExpensesByDateRange(
    Instant.fromEpochMilliseconds(startMillis),
    Instant.fromEpochMilliseconds(endMillis)
)
```
