# Expense Analyst — Current Status

**Date**: 2026-03-30
**DB version**: 10 (no changes this session)
**Build**: `./gradlew clean assembleDebug` ✅ passing
**Device**: Samsung Galaxy S26 Ultra (SM-S948B), ADB connected via wireless

---

## Current Phase

**Phase 1.5 complete — Phase 2 Analytics Dashboard complete (F12).**

Merchant Category Intelligence Engine shipped and verified working end-to-end on device.

---

## What Is Complete

### Core Infrastructure
- [x] Multi-module Clean Architecture (10 modules — added `feature:analytics`)
- [x] Room DB v10 with 8 entities and full migration history
- [x] Hilt DI wired across all modules
- [x] Jetpack Navigation Compose with all routes registered
- [x] Multi-currency: live rates (ExchangeRate-API via Ktor) + 40-rate offline seed
- [x] Home currency preference (DataStore)

### Expense Features
- [x] Manual expense entry (EXPENSE, INCOME, TRANSFER, PAYMENT types)
- [x] Expense list: date-grouped, search, category + payment filters, monthly navigation
- [x] Expense detail with "Teach App" auto-categorisation rule card
- [x] Edit expense (recalculates homeAmount on currency change)
- [x] Soft-delete with undo
- [x] EMI/instalment splitting (any expense → N monthly instalments with optional interest)
- [x] EMI list (active/completed tabs) and detail (timeline view, cancel remaining)

### Notification / SMS Pipeline
- [x] `TransactionNotificationService` (NotificationListenerService)
- [x] Pending inbox with badge count
- [x] In-app notification banner
- [x] System tray notification with tap-to-add
- [x] Bulk SMS import: last 30 days or all-time, with two-tier deduplication
- [x] Live notification dedup (60s window + expense body hash check)
- [x] Banner dismissed correctly after tray-tap → AddExpense → save flow

### SMS Parsers (17 parsers with full test coverage)
- [x] HDFC, SBI, ICICI, Axis, Kotak, Yes Bank, IDFC First Bank, OneCard (Indian banks)
- [x] Al Rajhi, STC Bank, Alinma, D360, Emirates NBD, Mubasher (Saudi/UAE banks)
- [x] FASTag, Wallet, UPI (payment channel parsers)
- [x] GenericParser (always-on fallback)

### Merchant Category Intelligence Engine ✅ complete this session
- [x] 3-tier inference: MerchantRules (instant) → Keyword matching → Google Places API
- [x] `InferCategoryUseCase` orchestrates all 3 tiers
- [x] `GooglePlacesApiService` — uses New Places API (`POST /v1/places:searchText`)
- [x] `MerchantSearchRepository` + `MerchantSearchRepositoryImpl`
- [x] Settings toggle: "Smart Category Detection" card (feature-gated, default off)
- [x] Settings: API key entry field with show/hide toggle, persisted in DataStore
- [x] AddExpense screen: loading spinner + "Suggested · tap to change" label
- [x] Bulk SMS import: Tier 3 web search with per-run in-memory cache + batch rule save
- [x] Verified on device: "Atypical" → `[coffee_shop, cafe, food]` → **Food** ✅

### Settings / UX
- [x] Onboarding (3-step: welcome → currency → notification permission)
- [x] Settings: home currency, notification toggle, SMS import, category management
- [x] Theme toggle (dark/light mode)
- [x] Category management screen (add, edit, delete, icon picker)
- [x] Bill detail drill-down screen
- [x] App icon: custom neon-cyan bar chart + magnifying glass
- [x] Smart Category Detection toggle + API key (Settings)

### Analytics Dashboard (Phase 2 — F12) ✅ complete
- [x] `feature/analytics` module
- [x] Month navigation, summary cards, category breakdown, daily bar chart, top merchants
- [x] Drill-down bottom sheet (tap any card/bar/merchant → see underlying expenses)
- [x] "View Analytics →" button on Home screen

---

## In Progress / Partially Done

| Item | State | Notes |
|------|-------|-------|
| Home currency hardcoded in ExpenseDetail | ⚠️ Bug | Line ~240 checks `!= "SAR"` instead of actual home currency |
| ProGuard/R8 rules | ⚠️ Missing | Release build unverified |

---

## Not Started (Phase 2 remaining)

- Budgets and overspend alerts
- CSV / PDF export
- Google Drive backup
- Home screen widget
- Bulk expense operations
- Email parsing
- Paging 3 integration

---

## Recommended Next Task

**Fix hardcoded home currency in `ExpenseDetailScreen`** (~line 240).

The screen checks `expense.currencyCode != "SAR"` to decide whether to show a second currency row. This should use the actual home currency from `CurrencyRepository.getHomeCurrency()`.

**Scope** (self-contained, no migration):
1. In `ExpenseDetailViewModel`, collect `homeCurrency: String` from `CurrencyRepository.getHomeCurrency()`
2. Expose it in `ExpenseDetailUiState`
3. In `ExpenseDetailScreen`, replace `!= "SAR"` with `!= uiState.homeCurrency`

**Agent**: FeatureAgent
**Estimated scope**: 3 files, no migration

---

## Known Constraints

- **JAVA_HOME**: Not set in shell profile. Build requires symlink workaround:
  `ln -sfn "/Applications/Android Studio.app/Contents/jbr/Contents/Home" /tmp/jbr_home && export JAVA_HOME=/tmp/jbr_home`
  Then use `bash gradlew ...` instead of `./gradlew`.
- **kotlinx.serialization compiler plugin**: NOT in the project. Do not use `@Serializable` data classes for Ktor responses in `:data` module — use `bodyAsText()` + `JsonElement` tree API instead. See skill `ktor-json-parsing-without-serialization-plugin.md`.
