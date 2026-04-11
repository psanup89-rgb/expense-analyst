# Expense Analyst — Current Status

**Date**: 2026-04-11
**DB version**: 13 (MIGRATION_12_13 — `reference TEXT` column on `bills`)
**Build**: `./gradlew clean assembleDebug` ✅ passing
**Device**: Samsung Galaxy S26 Ultra (SM-S948B) — installed ✅
**Version**: 0.1.0 (alpha)

---

## Current Phase

**Phase 1.5 + Phase 2 Analytics (F12) complete.**

All core infrastructure, SMS parsing, account management, bill tracking (with edit), and analytics dashboard are shipped.

---

## What Is Complete

### Core Infrastructure
- [x] Multi-module Clean Architecture (11 modules — includes `feature:analytics`)
- [x] Room DB v13 with 10 entities and full migration history (v1→v13)
- [x] Hilt DI wired across all modules (12 repository interfaces)
- [x] Jetpack Navigation Compose with all routes registered
- [x] Multi-currency: live rates (ExchangeRate-API via Ktor) + 40-rate offline seed
- [x] Home currency preference (DataStore)

### Expense Features
- [x] Manual expense entry (EXPENSE, INCOME, TRANSFER, PAYMENT types)
- [x] Expense list: date-grouped, search, category + payment filters, monthly navigation
- [x] Expense detail with "Teach App" auto-categorisation rule card
- [x] Edit expense (recalculates homeAmount on currency change)
- [x] Soft-delete with undo
- [x] EMI/instalment splitting + EMI list/detail (cancel remaining)
- [x] Tags system: reusable many-to-many tags, searchable, chips in Detail

### Notification / SMS Pipeline
- [x] `TransactionNotificationService` (NotificationListenerService)
- [x] Pending inbox with badge count, soft-duplicate warning ("Add Anyway")
- [x] In-app notification banner + system tray notification
- [x] Bulk SMS import: last 30 days or all-time, two-tier deduplication
- [x] Live notification dedup (60s window + expense body hash check)

### SMS Parsers
**Transaction parsers (18):** HDFC, SBI, ICICI, Axis, Kotak, YesBank, IdfcFirstBank, OneCard, AlRajhi, StcBank, Alinma, D360, EmiratesNBD, FASTag, Wallet, UPI, Mubasher, Generic

**Bill statement parsers (9):** HDFC, EmiratesNBD, AlRajhi, IdfcFirstBank, AxisBank, Tamara, SaudiEnergy, Ejar, Generic

### Bills
- [x] Bill tracking: PENDING → PARTIAL → SETTLED lifecycle
- [x] Bill detail screen (shows reference, amounts, status, due date, linked expenses)
- [x] Edit Bill screen (biller name, reference, amounts, due date, status)
- [x] `reference` field on `Bill` entity (v13) — stores account/contract number separately from biller name
- [x] Saudi Energy parser: account number → `reference`
- [x] Ejar parser (Arabic SMS): contract number → `reference`

### Account Management
- [x] Account Management screen (Settings → Manage Accounts)
- [x] Add / Edit accounts (bank name, last-4, account type)
- [x] Delete with remap: shows full scrollable expense list → choose target account or unassign
- [x] Tapping an expense in the delete dialog navigates to Edit Expense screen
- [x] Expense count shown in edit dialog subtitle
- [x] Account picker in AddExpense screen

### Merchant Category Intelligence Engine
- [x] 3-tier inference: MerchantRules → Keyword → Google Places API
- [x] API key at build time via `BuildConfig.GOOGLE_PLACES_API_KEY` (from `local.properties`)
- [x] Settings toggle: "Smart Category Detection" (default off)
- [x] Tier 3 gated in both `InferCategoryUseCase` and `SmsImportViewModel`

### Settings / UX
- [x] Onboarding (3-step), home currency, notification toggle, SMS import
- [x] Category management screen (add, edit, delete, icon picker)
- [x] Theme toggle (dark/light mode)
- [x] App icon: custom neon-cyan bar chart + magnifying glass

### Analytics Dashboard (Phase 2 — F12)
- [x] Month navigation, summary cards, category breakdown, daily bar chart, top merchants
- [x] Drill-down bottom sheet

---

## In Progress / Partially Done

| Item | State | Notes |
|------|-------|-------|
| ProGuard/R8 rules | ⚠️ Missing | Release build unverified |
| Hardcoded `"SAR"` in ExpenseDetailScreen | ⚠️ Bug | ~line 240 — should use `uiState.homeCurrency` |

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

## Known Constraints

- **`local.properties`** (gitignored): needs `GOOGLE_PLACES_API_KEY=AIzaSy...` for Tier 3 to work
- **kotlinx.serialization compiler plugin**: NOT in project. Use `bodyAsText()` + `JsonElement` for Ktor JSON, not `@Serializable` data classes
- **LazyColumn inside AlertDialog text slot**: renders nothing (zero height). Always use `Column + verticalScroll` for scrollable content in dialogs
