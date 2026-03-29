# Expense Analyst — Handoff Document

**Last updated**: 2026-03-30
**DB version**: 10 (no changes this session)
**Build status**: `./gradlew clean assembleDebug` ✅ passing
**APK**: Installed on Samsung Galaxy S26 Ultra (SM-S948B) via `adb installDebug`

---

## What Was Accomplished This Session

### Merchant Category Intelligence Engine — end-to-end implementation + verification

**Goal**: When a bank notification arrives and the user taps it, automatically identify the expense category from the merchant name, including unknown local merchants.

**3-Tier System:**
1. **Tier 1 — MerchantRules** (instant): user-defined rules saved from previous lookups
2. **Tier 2 — Keyword matching** (instant): `CategoryInference.infer()` across ~100 keywords
3. **Tier 3 — Google Places API** (async ~1s): `POST /v1/places:searchText` → `places.types` → category

**New files created:**
- `domain/.../repository/MerchantSearchRepository.kt` — pure Kotlin interface
- `domain/.../usecase/InferCategoryUseCase.kt` — orchestrates all 3 tiers, checks enabled flag
- `data/.../remote/GooglePlacesApiService.kt` — new Places API, `bodyAsText()` + `JsonElement` parsing
- `data/.../repository/MerchantSearchRepositoryImpl.kt` — maps Google place types to categories

**Files modified:**
- `AppPreferencesRepository` + `AppPreferencesRepositoryImpl` + `CurrencyPreferencesDataSource` — 4 new prefs (enabled flag + API key)
- `RepositoryModule` — `@Binds` for `MerchantSearchRepository`
- `AddExpenseUiState` — `isCategoryInferring`, `categoryInferenceSource`
- `AddExpenseViewModel` — inference job launched in `init`, cancelled on manual category select
- `AddExpenseScreen` — 3-state category row: spinner / suggested label / placeholder
- `SmsImportViewModel` — Tier 3 with `webSearchCache` + batch MerchantRule save after loop
- `SettingsScreen/ViewModel/UiState` — "Smart Category Detection" card with toggle + API key field

**Settings UX:**
- Feature defaults to OFF (pro-gating ready)
- Toggle enables Tier 3 lookups
- API key field with show/hide eye button, saved to DataStore on each keystroke

**Verified on device:**
```
Atypical → Google Places → [coffee_shop, cafe, food_store, food, ...] → Food ✅
```

### Three bugs fixed during debugging:

1. **`body<T>()` serialization error** — `kotlinx.serialization` compiler plugin is NOT in the project. `@Serializable` data classes generate no code. Fixed by switching to `bodyAsText()` + raw `JsonElement` API — no compiler plugin needed.

2. **Legacy Places API** — `GET .../findplacefromtext/json` returns `REQUEST_DENIED` for new API keys (deprecated). Fixed by switching to `POST https://places.googleapis.com/v1/places:searchText` with `X-Goog-Api-Key` header and `X-Goog-FieldMask: places.types`.

3. **Response structure changed** — old API: `candidates[].types`; new API: `places[].types`. Updated JSON path accordingly.

---

## What Was NOT Finished

Nothing incomplete — the feature is fully shipped and verified.

The `DuckDuckGoApiService.kt` remains in the codebase but is unused (the `MerchantSearchRepositoryImpl` no longer references it). It can be deleted in a cleanup pass if desired.

---

## First Action for Next Agent

Fix the hardcoded SAR in `ExpenseDetailScreen` (~line 240):

```kotlin
// Current (wrong):
if (expense.currencyCode != "SAR") { ... }

// Fix:
if (expense.currencyCode != uiState.homeCurrency) { ... }
```

Steps:
1. Read `feature/expenses/src/main/java/com/expenseanalyst/feature/expenses/ui/ExpenseDetailViewModel.kt`
2. Add `CurrencyRepository` injection, collect `getHomeCurrency()` flow
3. Add `homeCurrency: String = "SAR"` to `ExpenseDetailUiState`
4. Fix the condition in `ExpenseDetailScreen`
5. `./gradlew clean assembleDebug` and verify

---

## Surprises / Gotchas Discovered

### 1. `kotlinx.serialization` compiler plugin is absent
The `:data` module `build.gradle.kts` has no `kotlin("plugin.serialization")` and it's not in `libs.versions.toml`. Using `.body<MyClass>()` in Ktor throws `SerializationException: Serializer for class 'X' is not found` at runtime.

**Rule**: Always use `bodyAsText()` + `jsonElement.jsonObject[...]` for Ktor responses in `:data`. The existing `CurrencyApiService` works because `ExchangeRateResponse` uses primitive types + Map which have built-in serializers — but this is fragile. Any new response class with nested structures will fail without the plugin.

### 2. Google Places Legacy API deprecated for new projects
New API keys (created after mid-2024) only work with the **New Places API** (`places.googleapis.com/v1/...`). The legacy `maps.googleapis.com/maps/api/place/...` returns `REQUEST_DENIED`. Always use the new endpoint.

### 3. New Places API response structure
- Uses `places[]` not `candidates[]`
- API key goes in `X-Goog-Api-Key` header, not `?key=` query param
- Field selection via `X-Goog-FieldMask: places.types` header
- Request is a POST with JSON body: `{"textQuery": "merchant name"}`

### 4. JAVA_HOME path with spaces
`/Applications/Android Studio.app/...` contains a space, breaking `./gradlew`. Workaround:
```bash
ln -sfn "/Applications/Android Studio.app/Contents/jbr/Contents/Home" /tmp/jbr_home
export JAVA_HOME=/tmp/jbr_home
bash gradlew clean assembleDebug
```
