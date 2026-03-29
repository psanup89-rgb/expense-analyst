# Expense Analyst — Changelog

Format: `[Date] — Summary`

---

## 2026-03-29 — Analytics dashboard + drill-down + custom app icon

**Agent role**: FeatureAgent

**Work completed**:
- Created `feature:analytics` module (new Gradle module, wired into app via `settings.gradle.kts` + `app/build.gradle.kts`)
- `AnalyticsScreen`: month navigation, Spent/Income summary cards, category `LinearProgressIndicator` bars, daily spend `Canvas` bar chart, top 5 merchants list
- Drill-down bottom sheet: tapping Spent card, Income card, any category bar, or any merchant row opens `ModalBottomSheet` showing underlying expense rows with category icon, merchant, date, amount
- `DrillDownFilter` sealed class + 5-way `combine()` in `AnalyticsViewModel` drives reactive filtering
- "View Analytics →" `TextButton` added to monthly summary card on Home/Expense List screen
- `AnalyticsScreen` wired in `AppNavGraph` with `onExpenseClick` → `ExpenseDetailScreen` navigation from drill-down rows
- Custom neon-cyan app icon: dark navy background + bar chart + magnifying glass foreground, adaptive icon XMLs, `AndroidManifest.xml` updated

**Key fix discovered**: Kotlin cannot smart-cast delegated property (`by collectAsStateWithLifecycle()`) nullable fields. Capture to local `val` before null-check. Documented in `skills/delegated-property-smart-cast.md`.

**Key fix discovered**: `ExpenseListScreen` has an intermediate `ExpenseListContent` composable between the screen and `MonthlySummaryCard`. Any new lambda param must be threaded through all three layers.

---

## 2026-03-29 — Session wrap-up: memory system + skills extracted

**Agent role**: FeatureAgent
**Commits this wrap-up**: session wrap-up commit (docs + skills)

**Work completed**:
- Updated `STATUS.md` to reflect all session completions and set next task (live dedup)
- Rewrote `HANDOFF.md` with accurate DB v10 state and full session summary
- Extracted 4 new skills to `skills/` covering the non-obvious patterns discovered
- Created `skills/SKILLS.md` master index

**Key decisions**:
- Recommended "live notification dedup" as next task over "bill detail screen" — it's a correctness bug (phantom duplicates) vs a missing feature, and has narrower scope
- Kept `GenericStatementParser` strictly gated to avoid false positives on transaction SMS
- Documented 5-tab bottom nav as the Material 3 maximum — future top-level screens must replace or nest

---

## 2026-03-29 — Project memory system bootstrapped

**Event**: Initial project analysis and agent memory system setup.

**Files created**: `PROJECT.md`, `STATUS.md`, `AGENTS.md`, `CHANGELOG.md`, `OPEN_QUESTIONS.md`, `CONSTRAINTS.md`

**What was found**: Phase 1 + Phase 1.5 fully complete. 9-module Clean Architecture Android app with 163 source files. Room DB at v10. 17 transaction parsers + 4 bill statement parsers. Bills tracking feature fully wired (domain model, DB entity, screen, auto/manual linking). Bottom nav: Home · Inbox · Bills · EMI · Settings. Build passing, device connected (Samsung Galaxy S26 Ultra).

**Existing files preserved**: `CLAUDE.md`, `HANDOFF.md`, `README.md` — all read but not modified.

---

## 2026-03-29 — Inbox dismiss confirmation + Inbox tab restored

**Commits**: `710300f`, `3fe3c7d`

- Restored Inbox tab to bottom nav (was accidentally removed when Bills tab was added)
- Added `AlertDialog` confirmation before dismissing a single pending inbox item: *"This transaction has not been added to your expenses yet. Are you sure you want to dismiss it?"*
- Added `AlertDialog` confirmation before "Clear All" — shows count of items that would be lost
- `PendingInboxViewModel` refactored from direct `dismiss()`/`dismissAll()` to `requestDismiss()`/`confirmDismiss()`/`cancelDismiss()` pattern

---

## 2026-03-29 — Manual bill linking from expense detail

**Commit**: `c217c98`

- PAYMENT expenses now show a "Linked Bill" row in the detail view
- If not auto-linked: "Link to Bill" button opens `ModalBottomSheet` listing all PENDING/PARTIAL bills
- Selecting a bill sets `expense.billId`, updates bill status (SETTLED if paid ≥ totalDue, else PARTIAL)
- Shows "No open bills" (disabled) when no open bills exist
- `ExpenseDetailViewModel` extended with `BillRepository` + `ExpenseRepository` injection, `openBills` flow, `linkToBill()` function

---

## 2026-03-29 — Bill statement SMS parsing in bulk import

**Commit**: `a97f4d0`

- `SmsImportViewModel` now tries `BillStatementParserRegistry` when transaction parsing returns null
- Recognised bill statements create/update Bill records directly during import
- Import result screen shows "Bills detected: N" row in stats
- `BillStatementManager` injected into `SmsImportViewModel`

---

## 2026-03-28 — Bills tracking feature + MubasherParser (DB v9→v10)

**Commit**: `9516af4`

- `MubasherParser`: fixes Mubasher App bill payment SMS not triggering (GenericParser missed "Bill Payment" noun form). Body-fingerprint detection, always PAYMENT direction.
- `Bill` domain model + Room entity (`bills` table) + `expense.bill_id` FK column
- `BillRepository` + `BillRepositoryImpl` + `BillDao`
- `BillStatementParserRegistry` with 4 parsers: EmiratesNBD, AlRajhi, HDFC, Generic
- `BillStatementManager` singleton
- `SmsReceiver` + `TransactionNotificationService` fallback to bill statement parsing
- `AddExpenseViewModel` auto-links PAYMENT expenses to open bills on save
- `BillsScreen` with Pending + Settled sections, overdue indicator, manual Add Bill FAB
- `PaymentMethodDetector`, `EmiratesNbdParser`, `FasTagParser`, `IdfcFirstBankParser`, `OneCardParser` added

---

## 2026-03-28 — Payment method intelligence + raw SMS preview + pending inbox

**Commit**: `d795f15`

- `PaymentMethodDetector` centralised utility — all 17 parsers now set `paymentMethodName`
- Raw SMS body preview added to Add Expense and Expense Detail screens
- `pendingId` threaded through all three tap paths (banner, tray notification, inbox)
- Account matching: requires both bank name and last-4 when known
- Wallet payment detected → account upgraded to CREDIT_CARD in-place
- SMS import: smarter dedup (body hash primary, amount+day+merchant fallback)
- SMS import: payment method mapped from parsed result (previously hardcoded OTHER)
- 4 new parsers: EmiratesNBD, IDFC First Bank, FASTag, OneCard
- Parser fixes: Axis, HDFC, ICICI, SBI, Yes Bank, Al Rajhi
- DB v7→v8: `raw_body` on pending_notifications
- DB v8→v9: `payment_method` on pending_notifications

---

## 2026-03-28 — Initial commit (Phase 1 + 1.5 baseline)

**Commit**: `fdc5a06`

- Full Phase 1 implementation: expense CRUD, EMI splitting, SMS/notification pipeline, 13 parsers, multi-currency, onboarding, settings
- DB v6 with 7 entities (expenses, categories, emi_groups, currency_rates, accounts, merchant_rules, pending_notifications)
- "Teach App" merchant categorisation rules
- Bottom nav: Home · EMI · Settings
