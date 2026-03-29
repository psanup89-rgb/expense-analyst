# Expense Analyst — Handoff Document

**Last updated**: 2026-03-29
**DB version**: 9 (no migration this session)
**Build status**: `./gradlew clean assembleDebug` ✅ passing
**Device tested**: Samsung Galaxy S26 Ultra (SM-S948B), connected via ADB
**Branch**: `main` (all work pushed)

---

## What Was Built This Session (2026-03-29)

### 1. Analytics Dashboard Module (`feature:analytics`) — Phase 2 F12

New Gradle module with full analytics dashboard UI.

**New module files:**
- `feature/analytics/build.gradle.kts` — module config
- `feature/analytics/src/main/AndroidManifest.xml`
- `feature/analytics/src/main/java/com/expenseanalyst/feature/analytics/di/AnalyticsModule.kt`
- `feature/analytics/src/main/java/com/expenseanalyst/feature/analytics/ui/AnalyticsUiState.kt`
- `feature/analytics/src/main/java/com/expenseanalyst/feature/analytics/ui/AnalyticsViewModel.kt`
- `feature/analytics/src/main/java/com/expenseanalyst/feature/analytics/ui/AnalyticsScreen.kt`

**Wiring changes:**
- `settings.gradle.kts` — `include(":feature:analytics")`
- `app/build.gradle.kts` — `implementation(project(":feature:analytics"))`
- `core/navigation/NavRoutes.kt` — `const val ANALYTICS = "analytics"`
- `app/navigation/AppNavGraph.kt` — registered `AnalyticsScreen` composable with `onExpenseClick`
- `feature/expenses/ui/ExpenseListScreen.kt` — "View Analytics →" `TextButton` in monthly summary card

**Analytics screen features:**
- Month navigation (← / →, clamped to current month)
- Summary cards: Spent (NeonYellow), Income (NeonGreen), vs Last Month delta (NeonGreen/NeonRed)
- Category breakdown with colored `LinearProgressIndicator` bars
- Daily spending `Canvas` bar chart (NeonGreen bars, day labels)
- Top 5 merchants ranked list

### 2. Analytics Drill-Down Bottom Sheet

Tapping any summary card, category bar, or merchant row opens a `ModalBottomSheet` showing the underlying expenses.

**`DrillDownFilter` sealed class** (in `AnalyticsUiState.kt`):
```
Spent | Income | ByCategory(categoryName) | ByMerchant(merchantName)
```

**`AnalyticsViewModel`**: Changed 4-way `combine()` to 5-way adding `_drillDownFilter: MutableStateFlow<DrillDownFilter?>`. Computes `drillDownExpenses` and `drillDownTitle` reactively. Public API: `setDrillDown(filter)` / `dismissDrillDown()`.

**`AnalyticsScreen`**: Added `onExpenseClick: (Long) -> Unit` param. Clickable `SummaryCard`, `CategoryBar`, `MerchantRow`. `DrillDownSheet` composable with `DrillDownExpenseRow` items (category color circle, merchant, date·category, amount).

**Critical Kotlin fix**: `uiState` is obtained via `by viewModel.uiState.collectAsStateWithLifecycle()` (delegated property). Kotlin cannot smart-cast `uiState.drillDownTitle` to non-null inside an `if` block. Solution: capture `val drillDownTitle = uiState.drillDownTitle` before the null check.

### 3. Custom App Icon

Replaced default `@android:drawable/sym_def_app_icon` with custom neon-cyan analytics icon.

**Files created:**
- `app/src/main/res/drawable/ic_launcher_background.xml` — dark navy `#0A0E1F` + subtle teal/purple diagonal grid lines
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — 5 increasing-height bar chart bars + magnifying glass (circle stroke + dark fill + diagonal handle) in `#00DFFF`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive icon linking background + foreground
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — same

**File modified:**
- `app/src/main/AndroidManifest.xml` — `android:icon="@mipmap/ic_launcher"` + `android:roundIcon="@mipmap/ic_launcher_round"`

---

## What Was NOT Finished

| Item | Reason |
|------|--------|
| Live notification dedup | Identified as recommended next task; not in scope this session |
| Home currency hardcoded in ExpenseDetail | Existing bug, not addressed this session |
| ProGuard/R8 rules | Not in scope |
| Bill detail drill-down screen | Not requested |
| Parser tests for new parsers (Mubasher, EmiratesNBD etc.) | Not in scope |

---

## First Action for Next Agent

Read `STATUS.md` → implement **live notification dedup** in `PendingNotificationManager.enqueue()`.

Exact scope (self-contained, no DB migration):
1. Add `findByBodyHash(hash: Int): PendingNotification?` to `PendingNotificationRepository` interface and `PendingNotificationDao`
2. In `PendingNotificationManager.enqueue(parsed)`: compute `parsed.rawBody?.trim()?.hashCode()`, query for recent match (last 60s), skip if found
3. Also check saved expenses for same hash in `SMS_AUTO` source — skip if already added

---

## Gotchas and Non-Obvious Things

1. **Delegated property smart cast**: `val uiState by vm.uiState.collectAsStateWithLifecycle()` — Kotlin cannot smart-cast nullable properties on delegated vars inside `if` checks. Always do `val localTitle = uiState.drillDownTitle; if (localTitle != null) { … localTitle … }`. See `skills/delegated-property-smart-cast.md`.

2. **`combine()` with 5 flows**: `kotlinx.coroutines.combine` has typed overloads up to 5 parameters. AnalyticsViewModel uses all 5. Beyond 5, nest `combine()` calls.

3. **KSP smart cast bug**: Cross-module nullable property checks fail to compile. Extract to local `val` first. See `skills/ksp-cross-module-smart-cast.md`.

4. **KSP stale state**: Always `./gradlew clean assembleDebug`. Never just `assembleDebug` after adding/changing files.

5. **Bottom nav at Material 3 maximum**: Home · Inbox · Bills · EMI · Settings = 5 tabs. Any new top-level destination must replace an existing tab or live as a nested route.

6. **`windowInsets = WindowInsets(0,0,0,0)` on TopAppBars**: All screens with a TopAppBar must set this to avoid double status-bar padding. `ExpenseListScreen` has no TopAppBar — its title is the first `LazyColumn` item.

7. **New feature module checklist**: `build.gradle.kts`, `AndroidManifest.xml`, `include()` in `settings.gradle.kts`, `implementation(project())` in `app/build.gradle.kts`, route constant in `NavRoutes.kt`, composable in `AppNavGraph.kt`. See `skills/new-feature-module.md`.

8. **`ExpenseListContent` intermediate composable**: `ExpenseListScreen` delegates to `ExpenseListContent` which calls `MonthlySummaryCard`. All three layers must carry any new lambda param — easy to miss the middle layer.

---

## Handoff Checklist for Next Agent

1. Read `CLAUDE.md` (conventions, architecture, build commands)
2. Read this file and `STATUS.md`
3. Run `./gradlew clean assembleDebug` — confirm it passes
4. DB is at **v9** — next migration is `MIGRATION_9_10` (note: a previous session used v10, then was rolled back/branched — verify the actual DB version in `ExpenseAnalystDatabase.kt` before assuming)
5. Check `skills/` directory for relevant patterns before starting
6. Update `STATUS.md` and this file at end of session
