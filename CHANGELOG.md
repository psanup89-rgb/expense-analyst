# Expense Analyst — Changelog

Format: `[Date] — Summary`

---

## 2026-04-12 (session 2) — Bills section polish + expense↔bill navigation

**Agent role**: FeatureAgent

**Work completed**:

### Add Bill form parity with Edit Bill
- `AddBillSheetContent` expanded: added Reference field, Minimum Due field, Due Date picker, Status dropdown; Total Due now shows home currency as read-only suffix (currency input removed)
- `BillsUiState` gained 4 new fields: `newReference`, `newMinimumDue`, `newDueDateMillis`, `newStatus`
- `BillsViewModel` gained handlers for all 4 new fields + injects `CurrencyRepository`

### Bills always stored in home currency
- `BillsViewModel.saveNewBill()` uses home currency from `CurrencyRepository` — ignores any transient form currency value
- `PendingInboxViewModel.confirmSaveBill()` now injects `CurrencyRepository` and sets `currencyCode = homeCurrency` when creating Bill records from pending inbox
- `AddExpenseViewModel.saveExpense()` bill status comparison always uses `computedHomeAmount ?: parsedAmount` (no currency-matching detour)

### Expense detail ↔ Bill detail bidirectional navigation
- `ExpenseDetailUiState` gains `linkedBillId: Long?`; populated alongside existing `linkedBillName`
- "Linked Bill" row in PAYMENT expense detail is now a tappable `TextButton` showing the biller name; tapping navigates to `BillDetailScreen`
- `AppNavGraph` wires `onViewBill` on `ExpenseDetailScreen` → `billDetail(billId)`
- `BillDetailScreen.onViewPayment` now routes to `expenseDetail` (not `editExpense`) — tap payment row → read-only `ExpenseDetailScreen` → back to bill

### Unlink payment from bill
- `BillDetailViewModel.unlinkPayment(expenseId)`: clears `expense.billId`, recalculates bill status (PENDING if no linked payments remain, PARTIAL if some remain)
- `PaymentItem` in `BillDetailScreen` gains `LinkOff` icon button + confirm `AlertDialog`
- After unlinking, the expense's "Linked Bill" row reverts to showing "Link to Bill" button

---

## 2026-04-12 (session 1) — Bill SMS routing + PAYMENT bill linking (DB v14)

**Agent role**: ParserAgent / FeatureAgent / DataAgent

**Work completed**:

### Bill reminder SMS → pending inbox as BILL type (DB v13→v14)
- `BillStatementManager` changed from auto-saving bills silently to enqueueing `PendingNotification` with `pendingType = "BILL"`; sets `linkedBillId` if open bill already exists for that biller
- `MIGRATION_13_14`: adds 4 columns to `pending_notifications`: `pending_type TEXT NOT NULL DEFAULT 'TRANSACTION'`, `biller_name TEXT`, `due_date_millis INTEGER`, `linked_bill_id INTEGER`
- `PendingNotification` domain model gains 4 new nullable fields with defaults (backward-compatible)
- `PendingInboxScreen` shows `PendingBillItem` card for BILL-type items: biller name, amount, due date, "Add as Bill" or "Update Bill" action buttons
- `PendingInboxViewModel` gains `confirmSaveBill()`, `confirmUpdateBill()`, `requestSaveBill()`, `requestUpdateBill()` and cancel counterparts

### Routing fix: bill SMS no longer misclassified as spend transactions
- `TransactionNotificationService` now tries `BillStatementParserRegistry` **first** (before `ParserRegistry`); returns early if a bill statement matches
- `AirtelStatementParser` added: handles Airtel Wi-Fi/Postpaid/Broadband "bill of Rs.X is pending" SMS; registers in `BillStatementParserRegistry`
- `GenericParser` gains `billReminderPattern` guard: returns null for common bill-reminder phrases ("ignore if already paid", "bill of Rs.X is pending", "minimum amount due", "bill payment reminder")
- Bill statement parser count: **9 → 10** (IDFC, Axis, EmiratesNBD, AlRajhi, HDFC, Tamara, SaudiEnergy, Ejar, Airtel, Generic)

### PAYMENT bill linking in Add/Edit Expense screens
- When transaction type is `PAYMENT`, `AddExpenseViewModel` calls `loadBillsForLinking()`: fuzzy-matches open bills by merchant name and pre-populates `linkedBill` in state for user to see/change before saving
- "Linked Bill" section added to `AddExpenseContent` (shared by Add + Edit): shows auto-linked bill chip or "Link to a Bill" button; user can unlink or swap
- Bill picker `ModalBottomSheet`: lists all open (PENDING/PARTIAL) bills; tap to link
- On `saveExpense()` with a linked bill: updates bill status (SETTLED if paid ≥ totalDue, PARTIAL otherwise)
- `EditExpenseViewModel` gains same `onLinkBill`, `onUnlinkBill`, `showBillPicker`, `dismissBillPicker`, `onTransactionTypeChange` (bill-aware)

---

## 2026-04-11 (session 2) — Source SMS in Edit Expense + inline Add Category

**Agent role**: FeatureAgent

**Work completed**:

### Source SMS card in Edit Expense
- `EditExpenseViewModel` now populates `rawSmsBody` and `expenseSourceType` from the loaded expense into the shared `AddExpenseUiState`
- The "Source SMS" collapsible card (already in Add Expense) now appears in Edit Expense for all auto-imported expenses
- `expenseSourceType: SourceType?` added to `AddExpenseUiState` so the card shows "Auto-imported from SMS" for pre-existing expenses that have `rawSmsBody = null`

### "Open in Messages ↗" deep link
- `RawSmsPreviewCard` gains an "Open in Messages ↗" `TextButton` (visible when expanded)
- Queries `content://sms/inbox` by body text to find the sender's phone number; launches `Intent(ACTION_VIEW, "sms:${address}")` to open that conversation directly
- Falls back to opening the messaging app's main screen if the SMS is no longer in the inbox

### `rawSmsBody` persistence bug fix
- `AddExpenseViewModel.saveExpense()` was silently discarding `state.rawSmsBody` when building the `Expense` object → all notification-inbox expenses stored with `rawSmsBody = null`
- Fixed: `rawSmsBody = state.rawSmsBody` added to the constructor call
- Bulk SMS import (`SmsImportViewModel`) was already saving it correctly; only the notification-inbox path was affected

### Inline "Add new category" in category picker sheet
- `CategoryRepository` injected into both `AddExpenseViewModel` and `EditExpenseViewModel`
- 5 new methods on each ViewModel: `showAddNewCategoryForm`, `hideAddNewCategoryForm`, `onNewCategoryNameChange`, `onNewCategoryIconChange`, `saveNewCategory`
- 4 new fields on `AddExpenseUiState`: `isAddingNewCategory`, `newCategoryName`, `newCategoryIconName`, `isSavingCategory`
- Category sheet shows "+ Add new category" button above the search field; tapping reveals an inline form with name field + 5-col icon grid (15 icons); Save auto-selects the new category and closes the sheet
- No DB migration; no new navigation routes

---

## 2026-04-11 (session 1) — Saudi Energy + Ejar parsers + Bill.reference (DB v13)

**Agent role**: ParserAgent / DataAgent / FeatureAgent

**Work completed**: Saudi Energy bill parser, Ejar bill parser, `Bill.reference` field (DB v12→v13), Edit Bill screen, account delete with expense remap.

*(See HANDOFF.md session 1 for full details.)*

---

## 2026-03-31 — Axis parsers + soft-duplicate detection (DB v12)

**Agent role**: ParserAgent / DataAgent / FeatureAgent

**Work completed**:

### New: `AxisBankStatementParser`
- Detects "Payment of INR X for Axis Bank Credit Card no. XXYYYY is due on DD-MM-YY" → Bill
- Extracts totalDue, minimumDue, due date (dd-MM-yy format), card last-4

### `AxisParser` — 3 fixes
- Debit keyword: `\bdebited\b` → `\bdebit(?:ed)?\b` (catches "Debit INR X", "NACH debit")
- ACH merchant: `ACH-DR-MONTHLYSMALLCAS-000` → extracts "MONTHLYSMALLCAS"
- NACH merchant: `NACH debit towards MERCHANTNAME for INR` pattern

### Soft-duplicate detection (DB v11→v12)
- `MIGRATION_11_12`: `ALTER TABLE pending_notifications ADD COLUMN is_possible_duplicate INTEGER NOT NULL DEFAULT 0`
- `PendingNotificationManager`: checks saved expenses for same amount + merchant + calendar day
- `ParsedTransaction` + `PendingNotification` domain model: `isPossibleDuplicate` flag added
- `NotificationBanner`: amber warning + "Add Anyway" button label
- `PendingInboxScreen`: orange "⚠ Possible duplicate" badge + "Add Anyway" button

---

## 2026-03-31 — Crash fix + 4 parser improvements

**Agent role**: ParserAgent / DataAgent

**Work completed**:

### Crash fix — MIGRATION_10_11 broken SQL
- Removed two SQL statements in `MIGRATION_10_11` that referenced `expenses.note` (column that never existed; entity uses `description`)
- Error was: `IllegalStateException: Migration didn't properly handle: expenses`
- Migration table creation and default tag seeding are unaffected

### New: `IdfcFirstBankStatementParser`
- Detects IDFC "bill due by DD Month, YYYY" format → creates Bill with due date, Total Due, Min Due
- Handles full month name date format (`dd MMMM, yyyy`, `d MMMM yyyy`, etc.)
- Registered first in `BillStatementParserRegistry`

### Fix: EmiratesNBD "Credit Card: Credited"
- Added `creditedPattern` to detect credit card payment messages
- Added `creditedCardPattern` for `Card : XX4388;Credit Card Visa` format
- Returns `TransactionDirection.PAYMENT`

### New: `TamaraStatementParser`
- Detects Tamara (BNPL) payment reminders ("payment of X SAR for your ORDER due in N days")
- Computes due date from "due in N days" relative to `System.currentTimeMillis()`
- Extracts merchant from order description (e.g. "Samsung order" → biller "Tamara – Samsung")
- Registered in `BillStatementParserRegistry` before `GenericStatementParser`

### Fix: HdfcParser — "Spent" keyword
- Added `spent` to HDFC debit keyword regex
- Handles: `Spent Rs.2 On HDFC Bank Card 1041 At GOOGLE CLOUD On 2026-03-30`

---

## 2026-03-30 — Tags system + API key security + Tier 3 flag gate (DB v11)

**Agent role**: FeatureAgent / DataAgent

**Work completed**:

### Tags system (replaces `note` field)
- Removed `note: String?` from `Expense` domain model and `ExpenseEntity`
- New `Tag` domain model + `TagRepository` interface + `TagRepositoryImpl`
- New Room entities: `TagEntity` (tags table, unique index on name) + `ExpenseTagCrossRef` (junction, composite PK, CASCADE FKs)
- `TagDao` with insert-or-ignore + junction management + `@Transaction setTagsForExpense()`
- DB migration v10→v11: creates `tags` + `expense_tags` tables; pre-seeds 9 default tags (Recurring, One-time, Reimbursable, Tax Deductible, Personal, Business, Shared, Subscription, Essential); migrates existing `note` text to tag if non-blank
- `ExpenseWithCategory` — added `@Relation(associateBy = Junction(ExpenseTagCrossRef))` for eager tag loading
- `AddExpense` / `EditExpense` ViewModels + UiState — `selectedTags`, `availableTags`, `tagSearchQuery` replacing `note`
- `AddExpenseScreen` — new `TagSelector` composable: selected tags as `InputChip` with remove, search `OutlinedTextField`, `FilterChip` suggestions, "Create" chip for new tags
- `ExpenseDetailScreen` — `TagsDetailRow` private composable with `FlowRow` of `FilterChip`
- `ExpenseListViewModel` — search now matches `it.tags.any { tag -> tag.name.lowercase().contains(q) }`

### API key security
- Deleted API key UI from Settings (text field, show/hide, DataStore storage)
- Key now embedded at build time: `data/build.gradle.kts` reads `local.properties` → `BuildConfig.GOOGLE_PLACES_API_KEY`
- `MerchantSearchRepositoryImpl` checks `BuildConfig.GOOGLE_PLACES_API_KEY.isBlank()` as guard
- Deleted `getGooglePlacesApiKey()` / `setGooglePlacesApiKey()` from `AppPreferencesRepository`, impl, and DataSource

### Tier 3 gated in SMS import
- `SmsImportViewModel` now reads `isGooglePlacesEnabled().first()` at start of `startBulkImport()`
- Tier 3 web search wrapped in `if (isPlacesEnabled)` — onboarding import (toggle default OFF) skips Places calls entirely

### Version
- `versionName` changed from `"1.0.0"` to `"0.1.0"` (alpha)

**Key decisions**:
- `java.util.Properties` cannot be used in Gradle Kotlin DSL (the `java` identifier resolves to the plugin extension). Line-based file reading used instead.
- `@OptIn` on an outer composable does not reliably suppress experimental API errors in nested content lambdas. Extracted `TagsDetailRow` as a standalone private composable with its own `@OptIn(ExperimentalLayoutApi::class)`.
- Tags pre-seeded in both `MIGRATION_10_11` (for existing users) and `SeedDatabaseCallback.onCreate` (for fresh installs).

---

## 2026-03-30 — Merchant Category Intelligence Engine (Google Places API)

**Agent role**: FeatureAgent / DataAgent

**Work completed**:
- Implemented 3-tier merchant category inference system
- **Tier 1** (instant): `MerchantRule` DB lookup — user-defined rules always win
- **Tier 2** (instant): `CategoryInference.infer()` keyword matching across ~100 keywords
- **Tier 3** (async ~1s): Google Places API (New) — `POST /v1/places:searchText` with `places.types` field mask
- `InferCategoryUseCase` orchestrates all 3 tiers; checks `isGooglePlacesEnabled()` before Tier 3
- `GooglePlacesApiService` — `bodyAsText()` + `JsonElement` tree parsing (no compiler plugin needed)
- `MerchantSearchRepositoryImpl` — maps Google place type taxonomy to 7 app categories
- `AppPreferencesRepository` + `CurrencyPreferencesDataSource` — 4 new DataStore keys (enabled + api key)
- Settings "Smart Category Detection" card — toggle (default off, pro-gate ready) + API key field with show/hide
- `AddExpenseScreen` — 3-state category row: spinner / "Suggested · tap to change" / placeholder
- `SmsImportViewModel` — Tier 3 with `webSearchCache` (deduplicate per import run) + batch rule save
- Verified on device: "Atypical" → `[coffee_shop, cafe, food]` → **Food** ✅

**Key decisions**:
- Feature defaults to OFF — designed for pro-tier gating. Toggle + API key stored in DataStore.
- **`bodyAsText()` over `body<T>()`**: `kotlinx.serialization` compiler plugin absent from project. Using `.body<MyClass>()` throws `SerializationException` at runtime. All new Ktor response parsing uses `bodyAsText()` + `JsonElement` API.
- **New Places API** (not legacy): new Google Cloud API keys only work with `places.googleapis.com/v1/...`. Legacy `maps.googleapis.com/maps/api/place/findplacefromtext` returns `REQUEST_DENIED`.
- `DuckDuckGoApiService` retained in codebase but unused — superseded by Google Places.

**Debugging sequence**:
1. Confirmed `body<T>()` → `SerializationException` via logcat
2. Switched to `bodyAsText()` — fixed serialization; revealed `REQUEST_DENIED` from legacy API
3. Switched to new `POST /v1/places:searchText` endpoint — success, `coffee_shop` → `Food`

---

## 2026-03-29 — Parser fixes + notification banner + STATUS corrections

**Agent role**: FeatureAgent / ParserAgent

**Work completed**:
- Corrected STATUS.md: DB version v9 → v10, live dedup marked done (was already implemented)
- Added `TRANSFER` to `TransactionDirection` enum
- `AlRajhiParser`: added "Credit Transfer Internal" transfer format handling — detects `transferFingerprintPattern`, extracts `To:XXXX` → accountLast4, `From:NAME` → merchant, hardcodes `NET_BANKING`; also updates `canParse()` to match transfer body fingerprint
- `MubasherParser`: narrowed `bodyFingerprintPattern` — removed `Amount:SAR \d` (too broad, matched any SAR SMS); now requires `Biller:` or `Service:` (Mubasher-specific fields)
- `SmsImportViewModel`, `TransactionAlertNotification`, `NotificationBanner`: handle `TRANSFER` direction (label + mapping)
- Parser tests: `AlRajhiParserTest` — transfer parse + canParse fingerprint tests; `MubasherParserTest` — non-match test for transfer body + Service field test
- Fixed in-app banner persisting after tray-tap → AddExpense → save: `MainViewModel.dismissBanner()` + called from `AppNavGraph.onSaved`

**Key decisions**:
- MubasherParser fingerprint must require Mubasher-specific fields (`Biller:` or `Service:`), not just `Amount:SAR`. Documented in `skills/parser-mubasher-fingerprint.md`.
- Banner dismiss can't be in `AddExpenseViewModel` (cross-module boundary violation). Goes through `MainViewModel` in `:app` which can reach `PendingNotificationManager`. Documented in `skills/banner-dismiss-on-save.md`.
- `TRANSFER` direction update requires checklist across 3 files. Documented in `skills/transaction-direction-enum-extension.md`.

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
