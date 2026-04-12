# Expense Analyst — Current Status

**Date**: 2026-04-13
**DB version**: 14 (MIGRATION_13_14 — 4 new columns on `pending_notifications` for BILL pending type)
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
- [x] Room DB v14 with 10 entities and full migration history (v1→v14)
- [x] Hilt DI wired across all modules (12 repository interfaces)
- [x] Jetpack Navigation Compose with all routes registered
- [x] Multi-currency: live rates (ExchangeRate-API via Ktor) + 40-rate offline seed
- [x] Home currency preference (DataStore)

### Expense Features
- [x] Manual expense entry (EXPENSE, INCOME, TRANSFER, PAYMENT types)
- [x] Expense list: date-grouped, search, category + payment filters, monthly navigation
- [x] Expense detail with "Teach App" auto-categorisation rule card
- [x] Edit expense (recalculates homeAmount on currency change)
- [x] Edit expense shows "Source SMS" card (expandable, collapsible) for auto-imported expenses; "Open in Messages ↗" deep-links to the sender's conversation
- [x] `rawSmsBody` correctly persisted on save from the notification inbox path (`AddExpenseViewModel.saveExpense()`)
- [x] Inline "Add new category" form inside the category picker sheet (Add & Edit expense) — mirrors Add Account pattern; auto-selects the new category on save
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

**Bill statement parsers (10):** HDFC, EmiratesNBD, AlRajhi, IdfcFirstBank, AxisBank, Tamara, SaudiEnergy, Ejar, Airtel, Generic

### Bills
- [x] Bill tracking: PENDING → PARTIAL → SETTLED lifecycle
- [x] Bill detail screen (shows reference, amounts, status, due date, linked expenses)
- [x] Edit Bill screen (biller name, reference, amounts, due date, status)
- [x] Add Bill sheet matches Edit Bill: reference, total due, minimum due, due date picker, status dropdown
- [x] `reference` field on `Bill` entity (v13) — stores account/contract number separately from biller name
- [x] Saudi Energy parser: account number → `reference`
- [x] Ejar parser (Arabic SMS): contract number → `reference`
- [x] Airtel parser: "bill of Rs.X is pending" AND "Bill for your Airtel … has been generated" formats → bill (not spend); extracts due date when parseable
- [x] Bill reminder SMS → pending inbox as BILL type (not auto-saved, not spend): DB v14 adds `pending_type`, `biller_name`, `due_date_millis`, `linked_bill_id` to `pending_notifications`
- [x] Pending bill card shows collapsible "Source SMS" row — raw SMS body passed through `TransactionNotificationService` → `BillStatementManager` → `PendingNotification.rawBody`
- [x] Routing: `TransactionNotificationService` tries `BillStatementParserRegistry` first; `GenericParser` guards against bill-reminder phrases
- [x] PAYMENT expenses in Add/Edit screen show "Linked Bill" section: auto-matched by merchant name, manual link/unlink via bill picker
- [x] On save: bill status updated (SETTLED if paid ≥ totalDue, PARTIAL otherwise); always compared in home currency
- [x] Bills always stored in home currency; `BillsViewModel` and `PendingInboxViewModel` use `CurrencyRepository` for currency code
- [x] Expense detail → Bill detail: tappable "Linked Bill" row navigates to `BillDetailScreen`
- [x] Bill detail → Expense detail: tapping a payment row navigates to `ExpenseDetailScreen` (read-only)
- [x] Unlink payment from bill: `LinkOff` icon per payment row in bill detail; clears `expense.billId`, recalculates bill status

### Account Management
- [x] Account Management screen (Settings → Manage Accounts)
- [x] Add / Edit accounts (bank name, last-4, account type)
- [x] Delete with remap: shows full scrollable expense list → choose target account or unassign
- [x] Tapping an expense in the delete dialog navigates to Edit Expense screen
- [x] Expense count shown in edit dialog subtitle
- [x] Account picker in AddExpense screen

### Merchant Category Intelligence Engine
- [x] 3-tier inference: MerchantRules → Keyword → Claude AI
- [x] Claude API key + base URL at build time via `BuildConfig.CLAUDE_API_KEY` / `BuildConfig.CLAUDE_API_BASE_URL` (from `local.properties`)
- [x] Supports any Anthropic-compatible proxy endpoint (configurable base URL)
- [x] Uses `claude-haiku-4.5` model — fast and cheap for single-label classification
- [x] Response validated against known category names before accepting (hallucination guard)
- [x] Settings toggle: "Smart Category Detection — Use Claude AI" (default off)
- [x] Tier 3 gated in both `InferCategoryUseCase` and `SmsImportViewModel`
- [x] Auto-save MerchantRule when user manually picks a category in Add/Edit Expense (merchant non-blank) — enables Tier 1 match next time without explicit "Teach App" tap

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
| ProGuard/R8 rules | ✅ Rules created | `app/proguard-rules.pro` added; release APK not smoke-tested (requires signing config) |

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

- **`local.properties`** (gitignored): needs `CLAUDE_API_KEY=<key>` and `CLAUDE_API_BASE_URL=<url>` for Tier 3 to work. Values may be wrapped in double-quotes — the build script strips them automatically. Default base URL is `https://api.anthropic.com` if blank.
- **kotlinx.serialization compiler plugin**: NOT in project. Use `bodyAsText()` + `JsonElement` for Ktor JSON, not `@Serializable` data classes
- **LazyColumn inside AlertDialog text slot**: renders nothing (zero height). Always use `Column + verticalScroll` for scrollable content in dialogs
- **`rawSmsBody` on pre-existing notification-path expenses**: Expenses saved from the inbox *before* the 2026-04-11 fix have `rawSmsBody = null`. The Edit Expense screen shows "Auto-imported from SMS" for those (inferred from `sourceType`); the expandable SMS text only appears for expenses saved after the fix or via bulk SMS import.
- **"Unknown Bank" on pre-existing accounts**: Accounts already created with "Unknown Bank" bank name remain as-is (historical data); new accounts created by `GenericParser` after 2026-04-12 will use the sender-derived bank name instead.
