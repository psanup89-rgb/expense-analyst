# Expense Analyst — Current Status

**Date**: 2026-03-29
**DB version**: 10
**Build**: `./gradlew clean assembleDebug` ✅ passing
**Device**: Samsung Galaxy S26 Ultra (SM-S948B), ADB connected

---

## Current Phase

**Phase 1.5 complete — Phase 2 not started.**

All session work from 2026-03-28 to 2026-03-29 is merged to `main` and installed on device. Project memory system (PROJECT.md, STATUS.md, AGENTS.md, CHANGELOG.md, OPEN_QUESTIONS.md, CONSTRAINTS.md, skills/) bootstrapped this session.

---

## What Is Complete

### Core Infrastructure
- [x] Multi-module Clean Architecture (9 modules)
- [x] Room DB v10 with 8 entities and full migration history (v1→v10)
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
- [x] `SmsReceiver` (primary path — `RECEIVE_SMS` permission, works when app killed)
- [x] `TransactionNotificationService` (secondary path — push notifications from banking apps)
- [x] Pending inbox with badge count, persist until added or dismissed
- [x] Dismiss confirmation dialog (single item + "Clear All") ✅ this session
- [x] In-app notification banner
- [x] System tray notification with tap-to-add
- [x] Bulk SMS import: last 30 days or all-time, with two-tier deduplication
- [x] Bill statement SMS parsing during bulk import ✅ this session
- [x] Raw SMS preview in add/detail screens

### SMS Parsers (17 transaction + 4 bill statement + 1 Mubasher)
- [x] HDFC, SBI, ICICI, Axis, Kotak, Yes Bank, IDFC First Bank, OneCard (Indian banks)
- [x] Al Rajhi, STC Bank, Alinma, D360, Emirates NBD, Mubasher (Saudi/UAE banks) ✅ Mubasher this session
- [x] FASTag, Wallet, UPI (payment channel parsers)
- [x] GenericParser (always-on fallback)
- [x] `PaymentMethodDetector` shared utility (all parsers set payment method)
- [x] Bill statement parsers: Emirates NBD, Al Rajhi, HDFC, Generic

### Bills Feature ✅ complete this session
- [x] `Bill` domain model + Room entity (DB v10)
- [x] `BillRepository` + `BillRepositoryImpl` + `BillDao`
- [x] `BillStatementManager` — processes statement SMS → creates/updates Bill records
- [x] `BillsScreen` — Pending + Settled sections, due date, overdue indicator, manual add FAB
- [x] Auto-link: PAYMENT expenses auto-link to open Bills on save
- [x] Manual link: "Link to Bill" bottom sheet from expense detail ✅ this session

### UX
- [x] Onboarding (3-step: welcome → currency → notification permission)
- [x] Settings: home currency, notification access toggle, SMS import trigger
- [x] Bottom nav: Home · Inbox (badged) · Bills · EMI · Settings ✅ Inbox restored this session
- [x] Account matching: find-or-create by bank name + last-4
- [x] Agent memory system: PROJECT.md, STATUS.md, AGENTS.md, CHANGELOG.md, OPEN_QUESTIONS.md, CONSTRAINTS.md, skills/ ✅ this session

---

## In Progress / Partially Done

| Item | State | Notes |
|------|-------|-------|
| Settings screen | ⚠️ Partial | Currency and notification toggle work. Missing: theme toggle, category management |
| Live notification dedup | ⚠️ Missing | Bulk import is deduped; `SmsReceiver` / `TransactionNotificationService` live path is not |
| Parser test coverage | ⚠️ Partial | 4 of 17+ parsers have tests. Mubasher, EmiratesNBD, IDFC, OneCard, FASTag have no tests |
| Bill detail drill-down | ⚠️ Missing | Bills tab shows cards but tapping does not navigate to a detail screen |

---

## Not Started

- Live notification duplicate detection (recommended next task — see below)
- Bill detail screen
- Analytics dashboard (Phase 2)
- Budgets and overspend alerts (Phase 2)
- CSV / PDF export (Phase 2)
- Google Drive backup (Phase 2)
- Home screen widget (Phase 2)
- Bulk expense operations (Phase 2)
- Light mode / theme toggle
- Category management UI
- Paging 3 integration
- ProGuard / R8 rules for release build
- Email parsing (architecture discussed, not implemented)
- DAO / ViewModel / UI tests

---

## Recommended Next Task

**Fix live-notification duplicate detection** (`PendingNotificationManager.enqueue()`).

The live SMS path (`SmsReceiver` → `PendingNotificationManager`) has no deduplication. The same SMS (e.g. from dual-SIM retry or notification replay) can create multiple inbox entries. Bulk import has this solved; live does not.

**Scope** (self-contained, no DB migration):
1. In `PendingNotificationManager.enqueue()`, before calling `repository.save()`:
   - Query `PendingNotificationRepository.getAll().first()` for any entry with matching `rawBody` hash within the last 60 seconds
   - If found → skip (return without enqueuing or posting tray notification)
2. Also query `ExpenseRepository.getExpensesSnapshot()` for any `SMS_AUTO` expense with same body hash
   - If found → skip (already added by user)
3. Add a `findByBodyHash(hash: Int): PendingNotification?` method to `PendingNotificationRepository` and `PendingNotificationDao` for efficiency

**Agent**: FeatureAgent (touches `feature/notification/service/` and `domain/`)
**Estimated scope**: 3–4 files modified, 1 new DAO query

---

## Known Blockers

None. Build passes. Device connected and installable.
