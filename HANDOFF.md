# Expense Analyst — Handoff

**Last updated**: 2026-03-26
**Build**: `./gradlew clean assembleDebug` passes (DB v6)
**Status**: Phase 1 + Phase 1.5 (intelligence engine, system notifications, parser hardening) complete.

---

## What Is Fully Implemented

### Modules (9)
`:app` · `:core` · `:domain` · `:data` · `:feature:expenses` · `:feature:emi` · `:feature:notification` · `:feature:settings` · `:feature:onboarding`

### Screens
| Screen | Notes |
|--------|-------|
| Onboarding (3-step) | Welcome → currency → notification access. SMS bulk-import offered at end. |
| Expense List | Grouped, search, category+payment filters, month nav, swipe-delete + undo |
| Add Expense | Merchant * (mandatory), Description (optional), Note (optional). 4 types: Expense/Income/Transfer/Payment |
| Edit Expense | Auto-migrates old SMS expenses: if merchantName null, copies from description |
| Expense Detail | Type row, "Teach App" rule card (SMS_AUTO/NOTIFICATION_AUTO only), collapsible Original SMS section |
| EMI Create / List / Detail | Full EMI split with optional interest |
| SMS Import | Bulk import (all / last 30 days) from `content://sms/inbox`. Dedupes by (amount, day). |
| Settings | Home currency, notification toggle, about section |

### Data Layer (DB v6)
- **6 Room entities**: `ExpenseEntity`, `CategoryEntity`, `EmiGroupEntity`, `CurrencyRateEntity`, `AccountEntity`, `MerchantRuleEntity`
- **6 DAOs** with full CRUD + Flow reactive queries
- **7 repository interfaces** in `:domain` — includes `AccountRepository`, `MerchantRuleRepository`
- **DB migrations**: v1→v2→v3→v4→v5→v6 in `ExpenseAnalystDatabase.kt`
  - v6 added `merchant_rules` table (unique index on `merchant_pattern`)
  - Earlier migrations added: `accounts`, `merchant_name`/`raw_sms_body`/`account_id` columns to expenses

### Domain Model Changes (vs. initial)
- `Expense` has: `merchantName: String?`, `rawSmsBody: String?`, `accountId: Long?`
- `TransactionType`: `EXPENSE | INCOME | TRANSFER | PAYMENT`
- `AccountType`: `SAVINGS | CURRENT | CREDIT_CARD | DEBIT_CARD | FOREX_CARD | WALLET | OTHER`
- New models: `Account(id, bankName, lastFour, accountType, displayName)`, `MerchantRule(id, merchantPattern, categoryId, categoryName, createdAt)`

### Intelligence Engine (Merchant Rules)
- `MerchantRule` — user-defined pattern→category mapping, stored in DB
- `CategoryInference.infer()` checks user rules first (case-insensitive `contains`), then keyword matching
- "Teach App" card in `ExpenseDetailScreen` — visible for `SMS_AUTO`/`NOTIFICATION_AUTO` expenses with a merchant name
- `ExpenseDetailViewModel`: `existingRule: StateFlow<MerchantRule?>`, `saveRule()`, `deleteRule()`

### System (Tray) Notifications
- `TransactionAlertNotification.kt` in `feature/notification/service/` — posts Android system notification
- Uses `packageManager.getLaunchIntentForPackage()` (avoids circular dep from feature → app module)
- `MainViewModel.pendingRoute: StateFlow<String?>` — set by `MainActivity.handleIntent()`/`onNewIntent()`
- `AppNavGraph` observes `pendingRoute` → navigates to AddExpense pre-filled + consumes
- `POST_NOTIFICATIONS` permission declared; requested on Android 13+ in `MainActivity`
- `ic_notification.xml` uses `fillColor="#FFFFFF"` (not `?attr/colorControlNormal` — AAPT error in library modules)

### Parser Hardening
All parsers fixed for the `.takeIf { it.isNotBlank() }` bug (group 1 empty string not null when alt 2 matches):
- **AlRajhiParser**: handles `At:MERCHANT` and `At: MERCHANT` colon format; terminators: `Fee|Balance|Ref|Exchange|Country|Total|Available|on\s+\d`; `cardPattern` handles `By:1234` and `Card:1234`
- **AxisParser**: rewritten — detects SAR/USD/AED/EUR/GBP (was hardcoded INR); `accountPattern` handles `Fx Card XX9665`; added `atPattern` for POS; kept `transferToPattern` for UPI
- **AlinmaParser / StcBankParser / UpiParser**: same group-extraction bug fixed
- **GenericParser**: detects bill/card payment SMS → `TransactionDirection.PAYMENT`
- `feature/notification/build.gradle.kts`: added `testOptions { unitTests.all { useJUnitPlatform() } }` + JUnit Platform Launcher dep (tests were silently not running before)

### PAYMENT Transaction Type
- Purple (`#7C5CBF`) in list and detail screens
- Excluded from monthly Expense/Income summary totals (settling existing debt, not new spending)
- 4th segment in Add/Edit type selector

### Account Tracking
- `AccountRepository.findOrCreate(bankName, lastFour, accountType)` — idempotent
- Bulk SMS import uses an in-session `accountCache` map to avoid redundant DB calls
- Account type inferred from SMS body keywords: `fx card`/`forex card`/`prepaid card` → `FOREX_CARD`

---

## Tests
| File | Covers |
|------|--------|
| `CreateEmiFromExpenseUseCaseTest` | EMI math, interest calculation |
| `CurrencyConversionTest` | Same-currency, cross-currency, fallback |
| `HdfcParserTest` | HDFC SMS patterns |
| `AlRajhiParserTest` | SAR debit (classic + colon format), credit, non-transaction |
| `ParserRegistryTest` | End-to-end dispatch for 8 banks |

**Gaps**: No DAO tests, no ViewModel tests, no UI tests. `useJUnitPlatform()` was missing from `feature/notification` until this session — verify other feature modules have it too before writing new tests there.

---

## Known Remaining Gaps

| Gap | Notes |
|-----|-------|
| **Bills section** | Planned: track credit card statements + utility bills from SMS; match payments to open bills. Not started — needs new `Bill` model, DB migration, new screen. |
| **Duplicate detection (notifications)** | Bulk SMS import has (amount+day) dedup. Live notification capture does not. |
| **Paging** | Expense list loads all records; Paging 3 not integrated |
| **Settings incomplete** | Theme toggle, category management not implemented |
| **ProGuard/R8 rules** | Release build may fail to obfuscate correctly — not validated |
| **Test coverage** | DAO tests, ViewModel tests, more parser fixtures needed |
| **Offline rate entry** | No UI to manually enter exchange rate when no cached rate exists |

---

## Phase 2 Backlog
Analytics dashboard · Budgets & alerts · CSV/PDF export · Google Drive backup · Home screen widget · Bulk operations

## Phase 3 Backlog
iOS (KMP) · Smart categorization · Recurring detection · Receipt photos · Split expenses · Income tracking

---

## Handoff Checklist
1. Read `CLAUDE.md` for conventions before writing any code.
2. Run `./gradlew clean assembleDebug` after any structural changes.
3. Verify `./gradlew :feature:notification:testDebugUnitTest` passes after parser changes.
4. See `.claude/skills/build-verify.md` if you hit `KSP failed with exit code: PROCESSING_ERROR`.
5. Adding a new bank parser: follow `docs/NOTIFICATION_PARSING.md` SOP.
