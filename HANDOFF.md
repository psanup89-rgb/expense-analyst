# Expense Analyst — Handoff Document

**Last updated**: 2026-03-30
**DB version**: 11 (bumped from 10 this session)
**Build status**: `./gradlew clean assembleDebug` ✅ passing
**APK**: Installed on Samsung Galaxy S26 Ultra (SM-S948B) via `adb installDebug`
**App version**: 0.1.0 (alpha)

---

## What Was Accomplished This Session

### 1. API Key Security — Embed at Build Time

**Problem**: Google Places API key was stored in plain-text DataStore and visible in Settings UI. Any reverse-engineered APK could extract it.

**Solution**: Key moved to `local.properties` (gitignored). `data/build.gradle.kts` reads it using line-based parsing (not `java.util.Properties` — see Gotchas) and injects it as `BuildConfig.GOOGLE_PLACES_API_KEY`. The Settings API key input field and DataStore keys were deleted entirely.

**Files changed**:
- `data/build.gradle.kts` — added `buildFeatures { buildConfig = true }` + `buildConfigField`
- `data/.../remote/GooglePlacesApiService.kt` — removed `apiKey` param, reads `BuildConfig` directly
- `data/.../repository/MerchantSearchRepositoryImpl.kt` — removed `AppPreferencesRepository` dep, uses `BuildConfig`
- `domain/.../repository/AppPreferencesRepository.kt` — deleted `getGooglePlacesApiKey()`, `setGooglePlacesApiKey()`
- `data/.../repository/AppPreferencesRepositoryImpl.kt` — deleted both overrides
- `data/.../local/preferences/CurrencyPreferencesDataSource.kt` — deleted 2 functions + 1 pref key
- `feature/settings/.../ui/SettingsUiState.kt` — deleted `googlePlacesApiKey`, `isApiKeyVisible`
- `feature/settings/.../ui/SettingsViewModel.kt` — simplified combine, deleted 2 functions
- `feature/settings/.../ui/SettingsScreen.kt` — deleted entire API key UI block + params

### 2. Tier 3 Gated in SMS Import

**Problem**: `SmsImportViewModel.startBulkImport()` called `merchantSearchRepository.searchMerchantCategory()` without checking the `isGooglePlacesEnabled` flag. The onboarding import ran Tier 3 unconditionally even when the toggle was OFF.

**Solution**: Injected `AppPreferencesRepository`, read `isGooglePlacesEnabled().first()` once at start of `startBulkImport()`, wrapped the Tier 3 call in `if (isPlacesEnabled)`.

**File**: `feature/notification/.../ui/SmsImportViewModel.kt`

### 3. Tags System (DB v10 → v11)

**Problem**: The `note: String?` field on `Expense` duplicated the `description` field and had no reuse value across expenses.

**Solution**: Replaced with a many-to-many Tags system: reusable, searchable, creatable from the expense form.

**New entities/files**:
- `domain/.../model/Tag.kt` — `data class Tag(id: Long, name: String)`
- `domain/.../repository/TagRepository.kt` — interface with `getAllTags()`, `createTag()`, `setTagsForExpense()`, etc.
- `data/.../local/entity/TagEntity.kt` — Room entity with unique index on `name`
- `data/.../local/entity/ExpenseTagCrossRef.kt` — junction table with composite PK + FKs with CASCADE
- `data/.../local/dao/TagDao.kt` — queries, insert-or-ignore, junction table management
- `data/.../mapper/TagMapper.kt` — `toDomain()` / `toEntity()` extensions
- `data/.../repository/TagRepositoryImpl.kt` — insert-or-get pattern for `createTag()`

**Modified**:
- `ExpenseAnalystDatabase` — bumped to v11, added 2 new entities, `abstract tagDao()`, `MIGRATION_10_11` (creates tables, pre-seeds 9 default tags, migrates existing `note` → `tags`)
- `data/di/DatabaseModule` + `RepositoryModule` — new dao provider + binding
- `domain/.../model/Expense.kt` — removed `note`, added `tags: List<Tag>`
- `data/.../local/entity/ExpenseEntity.kt` — removed `note` field (column stays in SQLite, Room ignores)
- `data/.../local/relation/ExpenseWithCategory.kt` — added `@Relation(associateBy = Junction(...))`
- `data/.../mapper/ExpenseMapper.kt` — tags in/out
- `data/.../repository/ExpenseRepositoryImpl.kt` — calls `tagDao.setTagsForExpense()` on add/update
- `AddExpenseUiState` — removed `note`, added `selectedTags`, `availableTags`, `tagSearchQuery`
- `AddExpenseViewModel` + `EditExpenseViewModel` — injected `TagRepository`, added tag CRUD functions
- `AddExpenseScreen` — replaced note `NeonTextField` with `TagSelector` composable (InputChip + search + FilterChip suggestions + Create chip)
- `EditExpenseScreen` — wired 4 tag callbacks
- `ExpenseDetailScreen` — replaced `expense.note` block with `TagsDetailRow` private composable (FlowRow of FilterChips)
- `ExpenseListViewModel` — search now filters by `it.tags.any { tag -> tag.name.lowercase().contains(q) }`

### 4. Version 0.1.0

`app/build.gradle.kts` `versionName` changed from `"1.0.0"` to `"0.1.0"`.

---

## What Was NOT Finished

Nothing incomplete. All planned work shipped and build verified.

**Known dead code**: `DuckDuckGoApiService.kt` in `:data` — unused, can be deleted in a cleanup pass.

---

## First Action for Next Agent

Fix the hardcoded `"SAR"` in `ExpenseDetailScreen.kt` (~line 240):

```kotlin
// Current (wrong):
if (expense.currencyCode != "SAR") { ... }

// Fix needed:
if (expense.currencyCode != uiState.homeCurrency) { ... }
```

Steps:
1. Read `feature/expenses/.../ui/ExpenseDetailViewModel.kt` and `ExpenseDetailUiState.kt`
2. Inject `CurrencyRepository`, collect `getHomeCurrency()` into the uiState combine
3. Add `homeCurrency: String = "SAR"` to `ExpenseDetailUiState`
4. Fix the condition in `ExpenseDetailScreen`
5. `./gradlew clean assembleDebug` + install + verify by setting home currency to INR in Settings

---

## Surprises / Gotchas Discovered

### 1. `java.util.Properties` doesn't work in Gradle Kotlin DSL

In `build.gradle.kts`, the identifier `java` resolves to the `java {}` project extension (Java plugin config), NOT the `java.util` package. Using `java.util.Properties()` throws `Unresolved reference 'util'`.

**Fix**: Use line-based file reading:
```kotlin
val googlePlacesApiKey: String = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.readLines()
    ?.find { it.startsWith("GOOGLE_PLACES_API_KEY=") }
    ?.substringAfter("=")
    ?.trim()
    ?: ""
```
See skill: `buildconfig-secret-from-local-properties.md`

### 2. `@OptIn` on outer composable doesn't cover experimental API in nested content lambdas

When `FlowRow` (an `@ExperimentalLayoutApi` API) is called inside a `Column { ... }` or `Card { ... }` content lambda within a composable function annotated with `@OptIn(ExperimentalLayoutApi::class)`, the Compose compiler still raises an error for the lambda site.

**Fix**: Extract the experimental call into a small private composable function with its own `@OptIn` annotation.
See skill: `compose-experimental-in-nested-lambda.md`

### 3. Room Junction @Relation for many-to-many

`@Relation(associateBy = Junction(...))` must be on the property in the embedding class (`ExpenseWithCategory`), not in the entity itself. The junction entity needs both FKs as `@ColumnInfo`-annotated fields matching the column names in `associateBy`. If column names mismatch, Room generates incorrect SQL silently.
See skill: `room-many-to-many-tags.md`
