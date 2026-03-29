# Expense Analyst — Current Status

**Date**: 2026-03-29
**DB version**: 10 (confirmed in `ExpenseAnalystDatabase.kt`)
**Build**: `./gradlew clean assembleDebug` ✅ passing (verified this session)
**Device**: Samsung Galaxy S26 Ultra (SM-S948B), ADB connected via wireless

---

## Current Phase

**Phase 1.5 complete — Phase 2 Analytics Dashboard complete (F12).**

All work from this session is code-complete on `main`. APK not yet installed on device (signing key mismatch — user must manually uninstall existing app and reinstall debug APK).

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
- [x] Live notification dedup (60s window + expense body hash check) ✅
- [x] Banner dismissed correctly after tray-tap → AddExpense → save flow ✅ this session

### SMS Parsers (17 parsers with full test coverage)
- [x] HDFC, SBI, ICICI, Axis, Kotak, Yes Bank, IDFC First Bank, OneCard (Indian banks)
- [x] Al Rajhi, STC Bank, Alinma, D360, Emirates NBD, Mubasher (Saudi/UAE banks)
- [x] FASTag, Wallet, UPI (payment channel parsers)
- [x] GenericParser (always-on fallback)
- [x] Al Rajhi "Credit Transfer Internal" → TRANSFER type ✅ this session
- [x] MubasherParser fingerprint narrowed (no longer grabs all SAR amount SMS) ✅ this session
- [x] `TransactionDirection.TRANSFER` added to enum ✅ this session

### Settings / UX
- [x] Onboarding (3-step: welcome → currency → notification permission)
- [x] Settings: home currency, notification toggle, SMS import, category management
- [x] Theme toggle (dark/light mode)
- [x] Category management screen (add, edit, delete, icon picker)
- [x] Bill detail drill-down screen
- [x] App icon: custom neon-cyan bar chart + magnifying glass

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
| APK installation on device | ⚠️ Pending | Signing key mismatch — user must uninstall existing APK first |
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

The screen checks `expense.currencyCode != "SAR"` to decide whether to show a second currency row. This should use the actual home currency from `AppPreferencesRepository` (or `CurrencyRepository.getHomeCurrency()`).

**Scope** (self-contained, no migration):
1. In `ExpenseDetailViewModel`, collect `homeCurrency: String` from `CurrencyRepository.getHomeCurrency()`
2. Expose it in `ExpenseDetailUiState`
3. In `ExpenseDetailScreen`, replace `!= "SAR"` with `!= uiState.homeCurrency`

**Agent**: FeatureAgent
**Estimated scope**: 3 files, no migration

---

## Known Blockers

- **APK install**: Existing app on device was signed with a different key. User must manually uninstall from device settings before `adb install` can deploy the debug APK.
- **JAVA_HOME**: Not set in shell profile. Build requires `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` set before running Gradle. Next agent should set this or add it to the shell profile.
