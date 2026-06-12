# Expense Analyst — Next Steps

**Updated**: 2026-06-13

Priority order for Phase 2 remaining work.

---

## 1. CSV Export (F14) — High priority

**Goal**: Export a filtered expense list as a `.csv` file shareable via the Android share sheet.

**Scope**:
- Triggered from the expense list screen (overflow menu or FAB long-press)
- Respects current month/category/search filters
- Columns: date, merchant, category, amount, currency, homeAmount, homeCurrency, paymentMethod, account, type, tags
- Use `FileProvider` + `Intent.ACTION_SEND` for sharing
- PDF export is a stretch goal (requires a third-party PDF library or Android `PrintManager`)

**No new DB migration needed.**

---

## 2. Home Screen Widget (F16) — Medium priority

**Goal**: A 2×2 or 4×1 Glance widget showing current-month spend vs. last month.

**Scope**:
- Add `androidx.glance:glance-appwidget` dependency
- New `:feature:widget` module (or add to `:feature:expenses`)
- Read from `ExpenseRepository` directly — no separate ViewModel (Glance uses `GlanceAppWidget`)
- Tap widget → opens app at expense list

---

## 3. ProGuard Release Smoke-Test — Medium priority

**Goal**: Verify `assembleRelease` produces a working APK.

**Steps**:
1. Create `keystore.properties` (gitignored) with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`
2. Add signing config block to `app/build.gradle.kts`
3. Run `./gradlew assembleRelease`
4. Install and verify app launches, Room migrations run, Hilt wiring works

`app/proguard-rules.pro` already exists with rules for Kotlin, Hilt, Room, Ktor, DataStore, and Coroutines.

---

## 4. Paging 3 Integration — Low priority

Currently all expenses load into memory. At >10,000 records this will cause jank. Replace `getExpenses()` `Flow<List<...>>` with `Pager` + `PagingSource` in `ExpenseDao` + `ExpenseListViewModel`.

---

## Decisions Needed from Owner

- **PDF export**: include in F14 scope or defer indefinitely?
- **Release signing**: keystore location and whether to automate via CI
- **Widget**: 2×2 or 4×1 layout preference?
