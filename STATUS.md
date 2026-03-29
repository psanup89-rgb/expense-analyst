# Expense Analyst — Current Status

**Date**: 2026-03-29
**DB version**: 9 (no migration this session)
**Build**: `./gradlew clean assembleDebug` ✅ passing
**Device**: Samsung Galaxy S26 Ultra (SM-S948B), ADB connected

---

## Current Phase

**Phase 1.5 complete — Phase 2 Analytics Dashboard complete (F12).**

All work from this session is merged to `main` and installed on device.

---

## What Is Complete

### Core Infrastructure
- [x] Multi-module Clean Architecture (10 modules — added `feature:analytics`)
- [x] Room DB v9 with 7 entities and full migration history
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

### SMS Parsers (17 parsers with full test coverage)
- [x] HDFC, SBI, ICICI, Axis, Kotak, Yes Bank, IDFC First Bank, OneCard (Indian banks)
- [x] Al Rajhi, STC Bank, Alinma, D360, Emirates NBD, Mubasher (Saudi/UAE banks)
- [x] FASTag, Wallet, UPI (payment channel parsers)
- [x] GenericParser (always-on fallback)
- [x] Parser tests: all 17 parsers now have JUnit 5 parameterized tests ✅ this session

### Settings / UX
- [x] Onboarding (3-step: welcome → currency → notification permission)
- [x] Settings: home currency, notification toggle, SMS import, category management ✅ this session
- [x] Theme toggle (dark/light mode) ✅ this session
- [x] Category management screen (add, edit, delete, icon picker) ✅ this session
- [x] Bill detail drill-down screen ✅ this session
- [x] App icon: custom neon-cyan bar chart + magnifying glass ✅ this session

### Analytics Dashboard (Phase 2 — F12) ✅ complete this session
- [x] `feature/analytics` module (new Gradle module, wired into app)
- [x] Month navigation (← / → with current-month clamp)
- [x] Summary cards: Spent (NeonYellow), Income (NeonGreen), vs Last Month delta
- [x] Category breakdown with colored LinearProgressIndicator bars (clickable)
- [x] Daily spending Canvas bar chart (NeonGreen bars, day labels)
- [x] Top 5 merchants ranked list (clickable)
- [x] Drill-down bottom sheet: tap any card/bar/merchant → see underlying expenses
- [x] Drill-down expense rows navigate to ExpenseDetailScreen
- [x] "View Analytics →" button in monthly summary card on Home screen

---

## In Progress / Partially Done

| Item | State | Notes |
|------|-------|-------|
| Live notification dedup | ⚠️ Missing | Bulk import is deduped; live SMS path is not |
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
- Live notification duplicate detection (recommended next task)

---

## Recommended Next Task

**Fix live-notification duplicate detection** in `PendingNotificationManager` (or whichever service handles live SMS).

The live SMS path has no deduplication — same SMS from dual-SIM retry or notification replay creates duplicate inbox entries. Bulk import solved this with two-tier dedup (rawBody hash + amount+day+merchant fallback). Apply the same to the live path.

**Scope** (self-contained, no DB migration):
1. In `PendingNotificationManager.enqueue()`, compute `rawBody.trim().hashCode()`, query recent pending items for match in last 60s → skip if found
2. Also check saved expenses for same hash in SMS_AUTO source type → skip if already added
3. Add `findByBodyHash(hash: Int)` to `PendingNotificationRepository` + `PendingNotificationDao`

**Agent**: FeatureAgent
**Estimated scope**: 3–4 files, 1 new DAO query, no migration

---

## Known Blockers

None. Build passes. Device connected and installable.
