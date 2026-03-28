# Expense Analyst — Constraints & Conventions

Extracted from the codebase. These are deliberate patterns agents must follow.

---

## Fixed Tech Choices (Do Not Change)

| Choice | Why it's fixed |
|--------|---------------|
| Kotlin only — no Java | Established from day 1, all 163 source files are Kotlin |
| Jetpack Compose + Material 3 | All UI is declarative Compose; no XML layouts exist |
| Room (SQLite) | 10 migration versions already committed; switching ORMs is a full rewrite |
| Hilt for DI | `@HiltViewModel` on all ViewModels; `@AndroidEntryPoint` on services |
| KSP (not KAPT) | Already migrated; never add `kapt` dependencies |
| Kotlin Coroutines + Flow | All async uses `StateFlow`, `Flow`, `viewModelScope`; no RxJava |
| Single activity (`MainActivity`) | Navigation Compose drives everything; no multi-activity patterns |
| `StateFlow<UiState>` with data class | State is a single data class per screen, never a sealed interface |
| UTC epoch milliseconds for timestamps | All `Long` fields named `*Millis` or `*AtMillis`; no `Date`, `LocalDate`, or `ZonedDateTime` in entities |
| Soft delete only | `isDeleted: Boolean` flag; `hardDelete` methods do not exist and must not be added |

---

## Build Constraints

- **Always run `./gradlew clean assembleDebug`** — never just `assembleDebug`. KSP incremental processing is disabled (`ksp.incremental=false` in `gradle.properties`). Using incremental builds after adding new files causes stale symbol errors.
- **DB version is currently 10.** Any schema change must bump to 11 and add `MIGRATION_10_11` inline in `ExpenseAnalystDatabase.kt`. Never use `fallbackToDestructiveMigration()`.
- **After any migration**, run `./gradlew :data:kspDebugKotlin` to regenerate schema JSON in `data/schemas/`. Commit the new schema file.
- **Min SDK 26** — do not use APIs below API 26 without a version check.

---

## Naming Conventions

| Thing | Convention | Example |
|-------|-----------|---------|
| Classes / Composables | PascalCase | `ExpenseListScreen`, `BillCard` |
| Functions / variables | camelCase | `onAmountChange`, `selectedAccountId` |
| Constants | SCREAMING_SNAKE_CASE | `EXPENSE_LIST`, `MIGRATION_9_10` |
| Package names | `com.expenseanalyst.<module>.<layer>` | `com.expenseanalyst.feature.expenses.ui` |
| Screen files | `<Feature>Screen.kt` | `BillsScreen.kt` |
| ViewModel files | `<Feature>ViewModel.kt` | `BillsViewModel.kt` |
| UiState files | `<Feature>UiState.kt` | `BillsUiState.kt` |
| Route constants | ALL_CAPS in `NavRoutes` object | `NavRoutes.PENDING_INBOX` |
| DB column names | `snake_case` via `@ColumnInfo(name = "...")` | `bill_id`, `created_at_millis` |
| Entity classes | `<Model>Entity` | `BillEntity`, `ExpenseEntity` |
| Mapper files | `<Model>Mapper.kt` extension functions | `ExpenseMapper.kt` |

---

## Module Structure Patterns

Every screen module follows this file triplet:
```
<Feature>Screen.kt       — @Composable, no business logic
<Feature>ViewModel.kt    — @HiltViewModel, StateFlow<UiState>, coroutines
<Feature>UiState.kt      — data class only, no logic
```

Every `:feature` module must:
- Have `implementation(project(":domain"))` and `implementation(project(":core"))` in its `build.gradle.kts`
- Never import from `:data`
- Use `hiltViewModel()` to obtain ViewModels in composables

Every new domain concept needs:
- A data class in `domain/model/`
- A repository interface in `domain/repository/`
- An entity in `data/local/entity/`
- A DAO in `data/local/dao/`
- A repository impl in `data/repository/`
- A `@Binds` entry in `data/di/RepositoryModule.kt`
- A `provideFooDao()` in `data/di/DatabaseModule.kt`

---

## Parser Conventions

- Every parser implements `TransactionParser` (interface in `feature/notification/parser/`)
- Every parser must implement both `canParse(sender: String, body: String): Boolean` and `parse(sender: String, body: String): ParsedTransaction?`
- `canParse()` must never throw — it is called on every SMS
- Use `PaymentMethodDetector.detect(body)` for payment method inference — never inline detection
- Use body fingerprint (`bodyFingerprintPattern`) when the sender is unknown/numeric — see `AlRajhiParser` and `EmiratesNbdParser` as reference
- Register new parsers in `ParserRegistry` **before** `GenericParser` (which must always be last)
- **Two-group amount regex bug**: `groupValues[1]` is `""` not `null` when only group 2 matches. Always use `.takeIf { it.isNotBlank() }` on both groups
- Full SOP in `docs/NOTIFICATION_PARSING.md`

---

## Navigation Conventions

- All route constants defined in `core/navigation/NavRoutes.kt` — never hardcode route strings elsewhere
- All composable registrations in `app/navigation/AppNavGraph.kt`
- New bottom-nav destinations must be added to both `MainBottomNav.kt` items list AND the `showBottomNav` list in `MainActivity.kt`
- `TopAppBar` on inner screens must set `windowInsets = WindowInsets(0, 0, 0, 0)` to prevent double status-bar padding (outer `Scaffold` in `MainActivity` already handles it)
- `ExpenseListScreen` has no `TopAppBar` — title is the first `LazyColumn` item

---

## Currency Conventions

- All conversion math lives in `domain/util/CurrencyConversion.kt` — **single source of truth, never duplicate**
- ISO 4217 currency codes (`"INR"`, `"SAR"`, `"AED"`, `"USD"`) — never use symbols or locale-dependent names in storage
- Every expense stores both `amount` (original currency) and `homeAmount` (converted to home currency)
- Show both amounts when they differ (original + home currency conversion)
- Exchange rates cached in Room; `isStale()` triggers refresh after 24 hours

---

## Things Agents Must Never Do

1. **Never hard-delete records** — always soft-delete via `isDeleted = true`
2. **Never skip `clean` in the build** — always `./gradlew clean assembleDebug`
3. **Never duplicate currency conversion logic** — use `CurrencyConversion.kt`
4. **Never import `:data` from `:feature` modules** — violates Clean Architecture boundary
5. **Never add a parser that always returns `true` from `canParse()`** unless it is the designated last-resort fallback (currently `GenericParser` — there must be only one)
6. **Never use `java.util.Date` or `LocalDate` in Room entities** — use `Long` (UTC epoch ms)
7. **Never add KAPT** — project uses KSP exclusively
8. **Never call `fallbackToDestructiveMigration()`** — always write proper migrations
9. **Never add a nav route string directly in a composable** — always use `NavRoutes.CONSTANT`
10. **Never put a `TopAppBar` without `windowInsets = WindowInsets(0,0,0,0)`** on any screen other than `ExpenseListScreen`
