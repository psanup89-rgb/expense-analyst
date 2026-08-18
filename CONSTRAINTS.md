# Expense Analyst — Constraints & Conventions

---

## Fixed Tech Choices (Do Not Change)

| Choice | Why it's fixed |
|--------|---------------|
| Kotlin only — no Java | All source files are Kotlin |
| Jetpack Compose + Material 3 | All UI is declarative Compose; no XML layouts exist |
| Room (SQLite) | 20 migration versions committed; switching ORMs is a full rewrite |
| Hilt for DI | `@HiltViewModel` on all ViewModels; `@AndroidEntryPoint` on services |
| KSP (not KAPT) | Already migrated; never add `kapt` dependencies |
| Kotlin Coroutines + Flow | All async uses `StateFlow`, `Flow`, `viewModelScope`; no RxJava |
| Single activity (`MainActivity`) | Navigation Compose drives everything |
| `StateFlow<UiState>` with data class | State is a single data class per screen |
| UTC epoch milliseconds for timestamps | All `Long` fields; no `Date`, `LocalDate`, `ZonedDateTime` in entities |
| Soft delete only | `isDeleted: Boolean` flag everywhere |

---

## Build Constraints

- **Always `./gradlew clean assembleDebug`** — never bare `assembleDebug`. KSP incremental is disabled (`ksp.incremental=false`). Missing clean after new files causes stale symbol errors.
- **DB is currently v20.** Next migration = `MIGRATION_20_21` inline in `ExpenseAnalystDatabase.kt`. Never use `fallbackToDestructiveMigration()`.
- **After any migration**: run `./gradlew :data:kspDebugKotlin` to regenerate schema JSON. Commit the new schema file.
- **Min SDK 26** — no APIs below API 26 without a version check.

---

## Naming Conventions

| Thing | Convention | Example |
|-------|-----------|---------|
| Classes / Composables | PascalCase | `ExpenseListScreen`, `BillCard` |
| Functions / variables | camelCase | `onAmountChange`, `selectedAccountId` |
| Constants | SCREAMING_SNAKE | `EXPENSE_LIST`, `MIGRATION_9_10` |
| Package names | `com.expenseanalyst.<module>.<layer>` | `com.expenseanalyst.feature.expenses.ui` |
| Screen/ViewModel/UiState | `<Feature>Screen.kt` / `*ViewModel.kt` / `*UiState.kt` | — |
| Route constants | ALL_CAPS in `NavRoutes` object | `NavRoutes.PENDING_INBOX` |
| DB columns | `snake_case` via `@ColumnInfo(name = "...")` | `bill_id` |
| Entity classes | `<Model>Entity` | `BillEntity` |

---

## Module Structure Patterns

Every screen module: `*Screen.kt` (composable, no logic) + `*ViewModel.kt` (@HiltViewModel) + `*UiState.kt` (data class only).

Every `:feature` module:
- Must have `implementation(project(":domain"))` and `implementation(project(":core"))`
- Must NOT import from `:data`
- Uses `hiltViewModel()` in composables

New domain concept checklist:
- `domain/model/` — data class
- `domain/repository/` — interface
- `data/local/entity/` — entity
- `data/local/dao/` — DAO
- `data/repository/` — impl
- `data/di/RepositoryModule.kt` — `@Binds`
- `data/di/DatabaseModule.kt` — `provideFooDao()`

---

## Parser Conventions

- Every parser implements `TransactionParser` or `BillStatementParser`
- `canParse()` must never throw
- Use `PaymentMethodDetector.detect(body)` — never inline payment method detection
- Register before `GenericParser` / `GenericStatementParser` (which must always be last)
- **Two-group regex bug**: `groupValues[1]` is `""` not `null` when only group 2 matches. Always `.takeIf { it.isNotBlank() }` on both groups.
- Full SOP: `docs/NOTIFICATION_PARSING.md`

---

## Navigation Conventions

- Route constants: `core/navigation/NavRoutes.kt` only — never hardcode strings elsewhere
- Registrations: `app/navigation/AppNavGraph.kt` only
- New bottom-nav destinations: add to both `MainBottomNav.kt` items AND `showBottomNav` list in `MainActivity.kt`
- `TopAppBar` must set `windowInsets = WindowInsets(0, 0, 0, 0)` on all screens (outer Scaffold handles insets)
- Exception: `ExpenseListScreen` has no TopAppBar — title is first LazyColumn item

---

## Compose Gotchas (Don't Repeat)

- **`LazyColumn` inside `AlertDialog` text slot**: Items are not rendered (lazy list measures at zero height inside a scroll container). Use `Column + verticalScroll(rememberScrollState())` with `heightIn(max = Xdp)` instead.
- **`@OptIn` on outer composable**: Doesn't cover nested lambdas. Extract as private composable with its own `@OptIn`.
- **`kotlinx-datetime` classpath**: A `:feature` module that uses `Expense.date` (type `kotlinx.datetime.Instant`) directly must declare `implementation(libs.kotlinx.datetime)`. Transitive dependency is not sufficient.

---

## Currency Conventions

- All conversion math: `domain/util/CurrencyConversion.kt` — single source of truth
- ISO 4217 codes only (`"INR"`, `"SAR"`, `"AED"`, `"USD"`)
- Every expense stores `amount` (original) + `homeAmount` (converted)
- Show both amounts when they differ

---

## Notification Conventions

- The reply `PendingIntent` for a `RemoteInput` action must be **mutable**; the content (tap-body) intent stays `FLAG_IMMUTABLE`. Use `androidx.core.app.PendingIntentCompat`, never a bare `PendingIntent.FLAG_MUTABLE` (API 31 constant, minSdk is 26).
- After a `RemoteInput` reply the system leaves a progress spinner until the app re-notifies the same id or cancels it. Every code path in a reply receiver must terminate in a notify or a cancel.
- A `BroadcastReceiver` that writes to the DB must use `goAsync()` and `finish()` in a `finally` — without it the process can be killed mid-write.
- Never capture the receiver's `Context` into a coroutine that outlives `onReceive`; unwrap `context.applicationContext` first.

---

## Things Agents Must Never Do

1. Hard-delete records — always `isDeleted = true`
2. Skip `clean` in the build — always `./gradlew clean assembleDebug`
3. Duplicate currency conversion logic — use `CurrencyConversion.kt`
4. Import `:data` from `:feature` modules
5. Add a parser where `canParse()` always returns `true` (except designated last-resort fallback)
6. Use `java.util.Date` or `LocalDate` in Room entities
7. Add KAPT — project uses KSP exclusively
8. Call `fallbackToDestructiveMigration()`
9. Hardcode a nav route string in a composable
10. Add a `TopAppBar` without `windowInsets = WindowInsets(0,0,0,0)` (except `ExpenseListScreen`)
