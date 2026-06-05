# Expense Analyst — Handoff

**Last updated**: 2026-06-06
**DB version**: 18
**Build**: `./gradlew clean assembleDebug` ✅
**Repo**: `https://github.com/psanup89-rgb/expense-analyst` (public)
**Release**: v0.1.3-debug (GitHub Release with APK)

---

## Session Summary (2026-06-06) — Loan/Lent tracking (issue #10) + GenericParser fix (issue #14)

### 1. GenericParser — "authorized for use" CC SMS not detected (#14)

Three compounding bugs in `GenericParser` caused the SMS `"Your credit card XX9731 was authorized for use at ANTHROPIC* CLAUDE SUB on 2026-06-05 for the amount of USD 23.00. Your new available credit limit is SAR 26,517.65"` to fall through all parsers silently.

| Bug | Fix |
|-----|-----|
| "authorized" not in weak-debit keyword set | Added `authorized` to the `paid\|purchase\|sent` regex |
| `*` not in merchant character class → "ANTHROPIC" truncated | Added `*` to `[A-Za-z0-9 _\-&./]` char class; added `\s+on\s+\d` date boundary stop |
| Currency detection scanned whole body → picked SAR from credit-limit disclosure over USD from amount | Fixed: scan the amount-match text first, fall back to whole-body scan only if no match |

New `GenericParserTest` case covers the exact SMS (verifies amount=23.0, currency=USD, type=DEBIT, merchant="ANTHROPIC* CLAUDE SUB", accountLast4="9731"). All 14 tests pass.

**Files changed**: `GenericParser.kt`, `GenericParserTest.kt`

---

### 2. Loans/Lent tracking feature (#10)

New `:feature:loans` module. Tracks money lent to others — shows up as an outgoing amount, schedules WorkManager reminders, and on settlement auto-creates an INCOME+Refund expense that nets out of monthly totals (reuses existing refund netting in `ExpenseListViewModel`).

#### DB v18 — new `lent_items` table (MIGRATION_17_18)

16 columns: `id`, `person_name`, `amount`, `currency_code`, `home_amount`, `description`, `lent_date_millis`, `status` (PENDING/SETTLED), `settled_amount`, `settled_date_millis`, `linked_expense_id`, `settlement_expense_id`, `reminder_datetime_millis`, `is_deleted`, `created_at_millis`, `updated_at_millis`. Two indexes on `status` and `is_deleted`.

#### Domain / Data

- `domain/model/LentItem.kt` — data class + `LentStatus` enum (`PENDING`, `SETTLED`)
- `domain/repository/LentRepository.kt` — interface (6 methods)
- `data/local/entity/LentItemEntity.kt`, `LentItemDao.kt`, `LentItemMapper.kt`, `LentRepositoryImpl.kt`
- Wired into `DatabaseModule.kt`, `RepositoryModule.kt`

#### WorkManager reminders

- `LentReminderWorker` (`@HiltWorker`) — fires notification; guards against settled/deleted items
- `LentReminderScheduler` — `schedule(lentId, millis)` / `cancel(lentId)` via `OneTimeWorkRequest` with `initialDelay`
- `LentReminderNotification` — channel `"lent_reminders"`, `IMPORTANCE_DEFAULT`
- `ExpenseAnalystApp` now implements `Configuration.Provider` for `HiltWorkerFactory`; WorkManager auto-init disabled in `AndroidManifest.xml`

#### UI (3 screens)

- **LoanListScreen** — PENDING / SETTLED tab layout; FAB to add; taps to detail
- **AddLoanScreen** (also edit via `loanId`) — person name, amount + currency, description, lent date, optional reminder datetime
- **LoanDetailScreen** — amount/status display; "Mark Settled" dialog (creates INCOME+Refund); Set/Clear Reminder; Edit; Delete (soft-delete + cancel WorkManager)

#### Navigation + entry point

- `NavRoutes.kt`: `LOANS`, `LOAN_DETAIL`, `ADD_LOAN`, `EDIT_LOAN` constants + helper fns
- `AppNavGraph.kt`: 4 new composables registered
- `SettingsScreen.kt`: "Loans & Lending" card added (between Budget and Import sections)

#### Files added / changed

New: `feature/loans/` (11 files), `domain/model/LentItem.kt`, `domain/repository/LentRepository.kt`, `data/local/entity/LentItemEntity.kt`, `data/local/dao/LentItemDao.kt`, `data/mapper/LentItemMapper.kt`, `data/repository/LentRepositoryImpl.kt`, `data/schemas/.../18.json`

Modified: `ExpenseAnalystDatabase.kt` (v18, MIGRATION_17_18), `DatabaseModule.kt`, `RepositoryModule.kt`, `ExpenseAnalystApp.kt`, `AndroidManifest.xml`, `AppNavGraph.kt`, `NavRoutes.kt`, `SettingsScreen.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`

---

## Open Issues

All previously open issues (#4–#9, #11, #12, #13, #14, #10) are now resolved. No open issues.

---

## First Action for Next Agent

Suggested tasks in priority order:

1. **Device install** — connect Samsung Galaxy S26 Ultra, run `./gradlew installDebug`, verify Loans feature end-to-end: add a lent item with reminder, set reminder to 1 min ahead, confirm notification fires, mark as settled and check that monthly Spent decreases.

2. **Phase 2 remaining features** — CSV/PDF export (F14), home screen widget (F16).

3. **ProGuard release smoke-test** — `./gradlew assembleRelease` requires a signing config. Set up `keystore.properties` + signing block in `app/build.gradle.kts`.

---

## Session Summary (2026-05-11) — Bug fixes: GitHub Issues #4–#9, #11, #12

Sweep of every open issue in the tracker. Issue #10 (Loan/Lent tracking with reminders)
is deferred to a separate plan because it needs greenfield reminder infrastructure
(WorkManager) and a new Room table.

### 1. Parser gaps (#4 / #5 / #6 / #7 / #8 / #9)

- **AxisBankStatementParser** — `bodyFingerprint` and `totalDuePattern` now also match the
  newer Axis CC reminder format `"INR 10747.2 is due for payment on 10-05-26 towards
  Axis Bank CC no. XX4502. INR 215 will be debited from Axis Bank A/c no. XX0426 via auto
  debit."`. CC last-4 (XX4502) is now preferred over the auto-debit account last-4
  (XX0426). `dueDatePattern` accepts `due (for payment) on dd-MM-yy`. Issues #4 + #7.
- **KeetaParser** — refund regex now also catches order-cancellation SMS phrased as
  `"was canceled … will return"`. `canParse` now also runs when the body contains a
  `[Keeta]` prefix even if the sender is a generic short-code. Issues #5 + #8.
- **EmiratesNbdParser** — `bodyFingerprintPattern` extended to include
  `Credit\s+Card:\s*Credited`, so the SMS in #6 is detected without an ENBD sender ID.
  The existing `POS Reversal` path was already present but only fired when ENBD was the
  sender; same fingerprint widening fixes #9 (POS Reversal from generic short-code).

### 2. PAYMENT routing for credit-card payment confirmations (#6)

- `PendingNotificationManager.enqueue()` now (a) sets merchant to `"BillPayments"`
  whenever the parser returns `TransactionDirection.PAYMENT` with a null/blank merchant,
  and (b) auto-links the saved `PendingNotification` to an open bill from the same biller
  using the new strict `BillMatcher` (see #11 below).
- `AddExpenseViewModel` reads `pending.linkedBillId` from the pending record and
  pre-populates `linkedBill` / `linkedBillId` in the form state. It also defaults the
  category to `"Bills"` when transaction type is PAYMENT and no category has been chosen,
  so the inbox confirm flow ships with the correct defaults.
- Confirming the PAYMENT expense already runs the existing PENDING → PARTIAL / SETTLED
  lifecycle transition in `AddExpenseViewModel.saveExpense()` — no change needed there.

### 3. Strict bill auto-linker (#11)

- New `domain/util/BillMatcher.kt` — replaces the old "any biller substring match wins"
  behaviour that linked May payments to last month's bill. A bill matches only when:
  - billerName substring matches (either direction, case-insensitive), AND
  - `|payment − totalDue| ≤ max(5%·totalDue, 1.0)` OR the bill has a minimumDue and the
    payment covers it.
  - If totalDue is unknown and minimumDue is unknown → refuses to link.
- Wired into `AddExpenseViewModel.loadBillsForLinking()` (manual add path) and
  `PendingNotificationManager.enqueue()` (inbox path). `BillStatementManager` still
  matches new bill statements by biller alone — that's the correct behaviour for the
  "is there an open bill for this biller?" use case.
- 10 unit tests in `domain/src/test/.../BillMatcherTest.kt` cover exact match, ±5%
  tolerance, minimumDue path, merchant mismatch, blank merchant, no-amount bill, etc.

### 4. Refunds reduce monthly Spent (#12)

- `ExpenseListViewModel`: `monthDebit` now subtracts INCOME-with-category=`Refund` from
  the gross EXPENSE sum; `monthCredit` excludes refunds. Net spend is clamped at 0.
- `AnalyticsViewModel`: same subtraction applied to `totalExpense` and
  `prevMonthExpense`. `totalIncome` excludes refunds. Daily-spend bar chart and
  per-category breakdown still use raw EXPENSE so drill-downs reconcile.

### 5. Tests + infra

- Added `EmiratesNbdParserTest` cases for #6 and #9, new `AxisBankStatementParserTest`,
  new `KeetaParserTest`, new `BillMatcherTest`.
- `domain/build.gradle.kts` now declares `org.junit.platform:junit-platform-launcher`
  on the test runtime classpath — Gradle 9 requires it explicitly. Without it the
  whole `:domain:test` task fails to start (this had been silently masking the EMI
  interest-formula assertion mismatch in `CreateEmiFromExpenseUseCaseTest` — a
  pre-existing bug in the test's expected value, not in production code; left as-is
  here because it is out of scope for these issues).

### Files changed
- `feature/notification/parser/AxisBankStatementParser.kt`
- `feature/notification/parser/KeetaParser.kt`
- `feature/notification/parser/EmiratesNbdParser.kt`
- `feature/notification/service/PendingNotificationManager.kt`
- `feature/expenses/ui/AddExpenseViewModel.kt`
- `feature/expenses/ui/ExpenseListViewModel.kt`
- `feature/analytics/ui/AnalyticsViewModel.kt`
- `domain/util/BillMatcher.kt` (new)
- `domain/build.gradle.kts` (junit-platform-launcher)
- Tests: `AxisBankStatementParserTest`, `KeetaParserTest`,
  `EmiratesNbdParserTest` (extended), `BillMatcherTest`
- Docs: `CLAUDE.md`, `docs/DATA_MODELS.md`, `HANDOFF.md`

---

## Session Summary (2026-05-02) — Bug fixes: GitHub Issues #1 & #2, DB crash

### 1. GitHub Issue #1 — Duplicate expenses from live notifications

**Root cause (two separate gaps):**
- `PendingNotificationManager` Check 2 (body hash vs saved expenses) only filtered `SMS_AUTO`. Expenses confirmed from live notifications are `NOTIFICATION_AUTO`, so the same SMS arriving again was not blocked → duplicate pending notification → user confirms twice.
- `SmsImportViewModel` Tier 1 bulk dedup also only scanned `SMS_AUTO`. Running bulk import after confirming a live notification would re-import the same transaction.

**Fixes:**
- `PendingNotificationManager.kt` line 60: filter now includes `NOTIFICATION_AUTO`.
- `SmsImportViewModel.kt` (existingBodyKeys filter): now includes `NOTIFICATION_AUTO`.

### 2. GitHub Issue #2 — Two SMS messages not parsed

**SMS A — Mubasher bill payment (`Amount:SR 1320`):**
- `MubasherParser.amountPattern` only matched `SAR`, not the `SR` abbreviation some Mubasher SMS variants use.
- Fix: pattern updated to `(?:SAR|SR|ر\.س)`.

**SMS B — STC prepaid services payment:**
- `StcBankParser` `isDebit` required `paid|debited|sent|deducted`; "stc prepaid services payment" has none of these.
- Fix: added `services?\s+payment|prepaid` as additional debit indicators.

Both parser fixes covered by new unit tests in `MubasherParserTest.kt` and `StcBankParserTest.kt`.

### 3. Critical crash on launch — DB v16 migration

**Root cause:** `MIGRATION_14_15` created the `salary_entries` unique index as `idx_salary_month_year`. Room auto-generates index names as `index_<tableName>_<columns>`, so it expected `index_salary_entries_month_year`. The mismatch caused `IllegalStateException: Migration didn't properly handle: salary_entries` on launch for all devices that had upgraded from DB v14.

**Fix:** DB bumped to v16. `MIGRATION_15_16` drops the misnamed index and recreates it with the correct name. Migration is safe for all device states — `IF EXISTS`/`IF NOT EXISTS` guards make it a no-op on fresh installs. `MIGRATION_14_15` SQL also corrected for future consistency.

**Files changed:** `ExpenseAnalystDatabase.kt` (v15→v16, new MIGRATION_15_16, corrected MIGRATION_14_15 index name).

### Releases this session
- `v0.1.1-debug` — parser + dedup fixes
- `v0.1.2-debug` — DB crash fix (install this one)

Both APKs installed on both connected devices and verified crash-free.

---

## Session Summary (2026-04-29) — Budget feature (F13)

### Budget section — new `:feature:budget` module

Full budget tracking feature accessible from Settings, protected by biometric/device credential authentication.

**DB MIGRATION_14_15** creates two new tables:
- `salary_entries` — monthly salary with unique index on (month, year), optional link to INCOME expense
- `planned_expenses` — planned expense items per month with soft delete

**Features:**
- Salary tracking: manual entry, auto-detect from INCOME transactions, salary history view
- Planned expenses: add/edit/soft-delete with category, carry-forward from previous month
- Planned vs Actual: category comparison progress bars, unplanned expense flagging, summary card (total planned, actual, savings/overspend)
- Month navigation (← month →)
- Biometric gate: `BiometricPrompt` with `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` authenticators; falls through if no lock screen set

**New files (17):**
- Domain: `SalaryEntry.kt`, `PlannedExpense.kt`, `BudgetRepository.kt`
- Data: `SalaryEntryEntity.kt`, `PlannedExpenseEntity.kt`, `SalaryDao.kt`, `PlannedExpenseDao.kt`, `BudgetRepositoryImpl.kt`
- Feature: `BudgetScreen.kt`, `BudgetViewModel.kt`, `BudgetUiState.kt`, `BiometricHelper.kt`, `BudgetComponents.kt`, `BudgetCards.kt`, `BudgetDialogs.kt`, `build.gradle.kts`, `AndroidManifest.xml`

**Modified files:** `ExpenseAnalystDatabase.kt` (v15), `ExpenseDao.kt` (+getIncomeByDateRange), `DatabaseModule.kt`, `RepositoryModule.kt`, `NavRoutes.kt`, `AppNavGraph.kt`, `SettingsScreen.kt`, `app/build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml` (+biometric)

---

## Session Summary (2026-04-28) — Data security, bulk import fix, repo migration

### 1. Data Security Rules added to CLAUDE.md

Added a "Data Security Rules" section to CLAUDE.md (after Project Overview) codifying rules for handling SMS data, PII, credentials, and prompt injection defence. These rules apply to all agent sessions.

### 2. Bulk SMS import — bill statements no longer enqueued to pending inbox

**Bug**: Colleague reported that after bulk SMS import, items appeared in both the expenses list AND the pending notification inbox.

**Root cause**: `SmsImportViewModel.startBulkImport()` called `billStatementManager.process(statement)` for bill-type SMS, which always creates a `PendingNotification` with `pendingType="BILL"`. This is wrong for bulk import — bills should just be counted, not enqueued.

**Fix**: Removed `billStatementManager.process()` call and cleaned up the unused `BillStatementManager` injection from `SmsImportViewModel`. The `billsFound` counter still works so the import summary reports bill SMS correctly.

File changed: `feature/notification/src/main/java/com/expenseanalyst/feature/notification/ui/SmsImportViewModel.kt`

### 3. Git repo migrated to new GitHub account

- Remote changed from `anoop-p-ksa0043/expense-analyst` → `psanup89-rgb/expense-analyst`
- Repo made public
- GitHub releases created: `v1.0.0-debug`, `v1.0.1-debug` (with APK assets)
- `gh auth setup-git` configured for the new account

---

## Session Summary (2026-04-13) — Claude AI Tier 3, bill fixes, merchant memory

### 1. Airtel bill generation SMS — `AirtelStatementParser`

Previous fingerprint only matched "bill of Rs.X" and "bill payment reminder". Airtel's **bill generation** format ("Bill for your Airtel Wi-Fi 20025035912 has been generated. Amount to be paid: Rs 2119.28") was slipping through to `GenericParser`, which saw the word "paid" and classified it as a DEBIT spend.

Fixes:
- `bodyFingerprint` extended with `bill\s+for\s+your\s+airtel`
- `parse()` now uses a dedicated `totalDuePattern` (`amount to be paid: Rs X`) before falling back to raw Rs.X match
- `dueDatePattern` added to extract `Due Date: dd-MMM-yyyy` when present
- `GenericParser.billReminderPattern` extended with `bill.{0,70}has\s+been\s+generated` as defence-in-depth

### 2. Source SMS in pending bill card — `PendingInboxScreen`, `BillStatementManager`, `TransactionNotificationService`

`PendingBillItem` now shows a collapsible "Source SMS" row (identical expand/collapse UX to the expense inbox card).

Changes:
- `TransactionNotificationService`: passes `statement.copy(rawBody = effectiveBody)` so the raw SMS text travels into the pending record
- `BillStatementManager`: uses `statement.rawBody` (was hardcoded `null`)
- `PendingInboxScreen`: `PendingBillItem` renders an `AnimatedVisibility` SMS block when `item.rawBody` is non-null

### 3. Auto-save MerchantRule on manual category selection

Previously a rule was only saved when: (a) the user explicitly tapped "Teach App" in Expense Detail, or (b) Tier 3 web search returned a result.

Now: whenever the user manually picks a category in the **Add** or **Edit** Expense category sheet and the merchant name is non-blank, a `MerchantRule` is silently upserted. Tier 1 will catch that merchant instantly on all future transactions.

- `AddExpenseViewModel.onCategorySelect()` — launches `merchantRuleRepository.saveRule()`
- `EditExpenseViewModel.onCategorySelect()` — same; `MerchantRuleRepository` injected (new dep)

### 4. Tier 3 category inference: Google Places → Claude AI

Replaced the Google Places API (paid, geographic restriction) with a call to an Anthropic-compatible endpoint using `claude-haiku-4.5`.

| What | Before | After |
|---|---|---|
| Service class | `GooglePlacesApiService` (deleted) | `ClaudeApiService` (new) |
| Auth header | `X-Goog-Api-Key` | `Authorization: Bearer <key>` |
| API | Google Places Text Search | Claude Messages API `/v1/messages` |
| Model | — | `claude-haiku-4.5` |
| Config keys | `GOOGLE_PLACES_API_KEY` | `CLAUDE_API_KEY` + `CLAUDE_API_BASE_URL` |
| Category mapping | `mapPlaceTypesToCategory()` (place type → string) | Claude returns category name directly |
| Hallucination guard | — | Response validated against `VALID_CATEGORIES` set |

`local.properties` now requires:
```
CLAUDE_API_KEY=<key>
CLAUDE_API_BASE_URL=https://your-proxy.example.com
```
Quotes around values are stripped by `localProp()` in `data/build.gradle.kts`. Base URL defaults to `https://api.anthropic.com` if blank. The endpoint at `api.gameron.me` uses `Authorization: Bearer` (not `x-api-key`); this is handled in `ClaudeApiService`.

`InferenceSource.WEB_SEARCH` renamed to `AI_SEARCH`. Settings label updated to "Use Claude AI".

---

## Session Summary (2026-04-12, session 3) — Open issue cleanup

### 1. ProGuard/R8 rules — `app/proguard-rules.pro` created

File was entirely missing (referenced in `build.gradle.kts` but not present). Created with rules for:
- **Kotlin / JVM**: annotations, metadata, enum valueOf/values, companion objects
- **Kotlin Coroutines**: MainDispatcherFactory, CoroutineExceptionHandler, volatile fields
- **kotlinx.datetime**: full keep
- **Hilt / Dagger**: `@HiltViewModel`, `@AndroidEntryPoint`, `@Module`, `@Provides`, `@Binds`, generated component classes
- **Room**: `@Entity`, `@Dao`, `@Database`, `@TypeConverter`; explicit keeps on all `data.local.entity`, `data.local.dao`, and `domain.model` packages
- **Ktor + OkHttp**: full keep + dontwarn for Android engine dependencies
- **DataStore**: keep + dontwarn
- **Miscellaneous**: suppression of bouncycastle, conscrypt, openjsse build-time warnings
- `assembleDebug` ✅ after adding rules

### 2. "Unknown Bank" account names — fixed in `GenericParser`

`GenericParser.parse()` now calls `bankNameFromSender(sender)` instead of the hardcoded `bankName` property. The private method maps recognisable sender substrings to real bank names (same lookup table as `SmsImportViewModel.bankDisplayNameFromSender()`), extended to cover: HDFC, ICICI, Axis, SBI, Kotak, Yes Bank, IndusInd, PNB, Al Rajhi, Alinma, STC, D360, Emirates NBD, IDFC First, OneCard, Canara, Bank of Baroda, Union Bank, Citi, Amex, Paytm, Airtel. Falls back to "Unknown Bank" only if no pattern matches. Applies to both live notification path and bulk SMS import.

### 3. Stale comment — `BillStatementParserRegistry.kt` line 5

Updated comment from "Tried only after [ParserRegistry] returns null" → "Tried FIRST by [TransactionNotificationService], before [ParserRegistry]…" to match actual routing behaviour since 2026-04-12 session 1.

### 4. `DuckDuckGoApiService.kt` — already absent

File not found anywhere in the codebase. Either deleted in a prior commit or never committed. Issue closed with no action.

---

## Session Summary (2026-04-12, session 2) — Bills polish + home currency + navigation

### 1. Add Bill form parity with Edit Bill

`AddBillSheetContent` in `BillsScreen.kt` now shows the same fields as `EditBillScreen`: Reference, Total Due (with home-currency suffix), Minimum Due, Due Date (DatePicker), Status (dropdown). `BillsUiState` gained 4 new fields; `BillsViewModel` gained corresponding handlers.

### 2. Bills always stored in home currency

- `BillsViewModel` injects `CurrencyRepository`; `init` block and `saveNewBill()` always use home currency code from repository (currency input removed from Add Bill form)
- `PendingInboxViewModel.confirmSaveBill()` now injects `CurrencyRepository` and uses home currency when creating Bill records
- `AddExpenseViewModel.saveExpense()` bill status comparison always uses `computedHomeAmount ?: parsedAmount` (no currency-matching detour)

### 3. Bidirectional expense ↔ bill navigation

- `ExpenseDetailUiState` gains `linkedBillId: Long?`
- "Linked Bill" row in PAYMENT expense detail is a tappable button → navigates to `BillDetailScreen`
- `AppNavGraph` wires `onViewBill` on `ExpenseDetailScreen`; payment tap in `BillDetailScreen` goes to `ExpenseDetailScreen` (not edit)

### 4. Unlink payment from bill

- `BillDetailViewModel.unlinkPayment(expenseId)`: clears `expense.billId`, recalculates bill status (PENDING / PARTIAL)
- `PaymentItem` gains `LinkOff` icon button + confirm dialog
- After unlink, the payment's "Linked Bill" row reverts to "Link to Bill" button

---

## Session Summary (2026-04-12, session 1) — Bill SMS routing + PAYMENT bill linking (DB v14)

### 1. Bill reminder SMS → pending inbox as BILL type

`BillStatementManager` no longer auto-saves bills silently. It now enqueues a `PendingNotification` with `pendingType = "BILL"`, `billerName`, `dueDateMillis`, `linkedBillId` (if open bill found).

**DB MIGRATION_13_14** adds 4 columns to `pending_notifications`:
```sql
ALTER TABLE pending_notifications ADD COLUMN pending_type TEXT NOT NULL DEFAULT 'TRANSACTION'
ALTER TABLE pending_notifications ADD COLUMN biller_name TEXT
ALTER TABLE pending_notifications ADD COLUMN due_date_millis INTEGER
ALTER TABLE pending_notifications ADD COLUMN linked_bill_id INTEGER
```

`PendingInboxScreen` branches on `item.pendingType`: BILL items render `PendingBillItem` (biller, amount, due date, "Add as Bill" / "Update Bill" actions); TRANSACTION items render the existing card unchanged. `PendingInboxViewModel` gained `confirmSaveBill()` / `confirmUpdateBill()` with confirm dialogs.

### 2. Routing fix — bill SMS no longer misclassified as spend

- `TransactionNotificationService` now tries `BillStatementParserRegistry` **first** (before `ParserRegistry`)
- `AirtelStatementParser` added (10th bill parser): handles Airtel Wi-Fi/Postpaid/Broadband "bill of Rs.X is pending" SMS
- `GenericParser` guards against bill-reminder phrases via `billReminderPattern` (returns null for "ignore if already paid", "bill of Rs.X is pending", "minimum amount due", "bill payment reminder")

### 3. PAYMENT bill linking in Add/Edit Expense

- `loadBillsForLinking()` in both `AddExpenseViewModel` and `EditExpenseViewModel`: called when type = PAYMENT, fuzzy-matches open bills by merchant name, pre-populates `linkedBill` in state
- "Linked Bill" section in `AddExpenseContent`: shows auto-linked bill chip or "Link to a Bill" button; user can unlink or swap via bill picker `ModalBottomSheet`
- `saveExpense()` updates bill status on save (SETTLED / PARTIAL), always comparing in home currency

---

## Session Summary (2026-04-11, session 2)

### 1. Source SMS in Edit Expense + "Open in Messages" deep link

- `EditExpenseViewModel` now populates `rawSmsBody` and `expenseSourceType` in the form state from the loaded `Expense`.
- `AddExpenseContent` (shared by Add & Edit) shows the "Source SMS" card when `rawSmsBody != null` **or** `sourceType != MANUAL`. Pre-existing auto-imported expenses with no stored body show "Auto-imported from SMS" instead of the expandable text.
- `RawSmsPreviewCard` enhanced: when expanded, an "Open in Messages ↗" `TextButton` appears. It queries `content://sms/inbox` by body text to find the sender's number, then launches `Intent(ACTION_VIEW, "sms:${address}")`. Falls back to opening the messaging app's main screen if no match found. `READ_SMS` permission was already declared.

### 2. `rawSmsBody` persistence bug fix

`AddExpenseViewModel.saveExpense()` was building the `Expense` object without `rawSmsBody`, so expenses added from the notification inbox were stored with `rawSmsBody = null` in the DB. Fixed by adding `rawSmsBody = state.rawSmsBody` to the `Expense(...)` constructor call. Bulk SMS import (`SmsImportViewModel`) was already correct.

### 3. `expenseSourceType` added to `AddExpenseUiState`

New field `expenseSourceType: SourceType? = null` — populated by `EditExpenseViewModel` from `expense.sourceType`. Allows the SMS card to show "Auto-imported from SMS" for pre-existing expenses that have no stored body text.

### 4. Inline "Add new category" in category picker sheet

Both Add Expense and Edit Expense now have an inline "Add new category" form inside the category bottom sheet, mirroring the existing "Add new account" pattern:

- "**+ Add new category**" `TextButton` appears above the search field.
- Tapping it shows an inline form: category name field + 5-column icon grid (15 icons, same set as Category Management) + Cancel / Save.
- On Save: `categoryRepository.addCategory()` is called; new category is **auto-selected** and the sheet closes.
- Both `AddExpenseViewModel` and `EditExpenseViewModel` have `CategoryRepository` injected and identical 5 methods: `showAddNewCategoryForm()`, `hideAddNewCategoryForm()`, `onNewCategoryNameChange()`, `onNewCategoryIconChange()`, `saveNewCategory()`.
- `AddExpenseUiState` gained 4 fields: `isAddingNewCategory`, `newCategoryName`, `newCategoryIconName`, `isSavingCategory`.
- No DB migration required.

---

## Session Summary (2026-04-11, session 1)

### 1. Saudi Energy + Ejar Bill Parsers

Two new `BillStatementParser` implementations:

**`SaudiEnergyStatementParser`** — English SMS
- Fingerprint: `se.com.sa` or `your bill for account` in body
- Extracts: amount from `"amount of X SAR"`, account number from `"account XXXXXXXXXX"`
- Sets `billerName = "Saudi Energy"`, `reference = accountNumber`

**`EjarStatementParser`** — Arabic SMS
- Fingerprint: `منصة إيجار` or `checkout.ejar.sa` in body
- Extracts: amount from `بقيمة X ريال`, contract from `عقد رقم XXXXXXXXXX`
- Sets `billerName = "Ejar"`, `reference = contractNumber`

Both registered in `BillStatementParserRegistry` before `GenericStatementParser`. Bill parser count: **9 total**.

---

### 2. `Bill.reference` Field (DB v12 → v13)

New nullable `reference: String?` field on `Bill` (domain), `BillEntity` (data), `ParsedBillStatement` (parser DTO).

**Migration** (`MIGRATION_12_13`):
```sql
ALTER TABLE bills ADD COLUMN reference TEXT
```

**Full stack threaded through**: `BillEntity` → `BillRepositoryImpl` (both mappers) → `BillStatementManager` (new bills + updates) → `BillDetailScreen` (shows "Reference: #XXXXXXXXXX" row when non-null).

---

### 3. Edit Bill Screen

New `EditBillScreen` / `EditBillViewModel` / `EditBillUiState` in `feature/expenses`.

Fields: biller name, reference, total due + currency, minimum due, due date (DatePicker), status (PENDING/PARTIAL/SETTLED dropdown).

Route: `NavRoutes.EDIT_BILL = "edit_bill/{billId}"` / `NavRoutes.editBill(billId)`.
`BillDetailScreen` has an edit pencil icon in the TopAppBar → navigates to `EditBillScreen`.

---

### 4. Account Management — Delete with Expense Remap

`AccountManagementScreen` (Settings) now fully implements delete with remap:

- Delete dialog loads full expense list via `ExpenseRepository.getExpensesByAccount()`
- Shows merchant, date, amount for each linked expense in a scrollable list
- Each expense row is tappable → navigates to `EditExpenseScreen` for review
- Remap dropdown: choose another account or "Unassign (no account)"
- On confirm: `ExpenseRepository.remapAccount(fromId, toId?)` then `AccountRepository.deleteAccount(id)`

**New DAO methods added** (`ExpenseDao`):
- `getExpensesByAccount(accountId)` — `@Transaction` query, returns `List<ExpenseWithCategory>`
- `countByAccount(accountId)` — for edit dialog subtitle
- `remapAccount(fromAccountId, toAccountId?)` — bulk UPDATE

**Compose gotcha discovered**: `LazyColumn` inside `AlertDialog`'s `text` slot renders nothing. Fix: use `Column + verticalScroll(rememberScrollState())` with `heightIn(max = Xdp)`.

**`feature/settings/build.gradle.kts`**: Added `implementation(libs.kotlinx.datetime)` — required because `Expense.date` is `kotlinx.datetime.Instant` and the settings module now uses it directly.

---

## Open Issues

GitHub Issues #4, #5, #6, #7, #8, #9, #11, #12 resolved this session (2026-05-11).
All issues (#4–#9, #11, #12) resolved this session. **Issue #10** (Loan/Lent tracking)
resolved in the 2026-06-06 session. **Issue #13** (wallet/POS pattern) resolved in a
prior session. **Issue #14** (GenericParser CC auth) resolved in the 2026-06-06 session.

---

## First Action for Next Agent

No critical bugs. Suggested next tasks in priority order:

1. **Phase 2 remaining features** — CSV/PDF export (F14), home screen widget (F16).

2. **ProGuard release smoke-test** — `./gradlew assembleRelease` requires a signing config. Set up `keystore.properties` + signing block in `app/build.gradle.kts` and verify release APK starts on device without crashing.

---

## Gotchas / Surprises

- **`LazyColumn` in AlertDialog**: Always use `Column + verticalScroll` for any scrollable content inside an `AlertDialog`'s text slot.
- **`LazyVerticalGrid` in `ModalBottomSheet`**: Works fine with a fixed `height(Xdp)` constraint — use this pattern for icon pickers inside sheets.
- **`kotlinx-datetime` classpath**: If a `:feature` module uses `Expense.date` (type `Instant`) directly, it needs `implementation(libs.kotlinx.datetime)` — transitivity is not sufficient.
- **Two-group regex**: `groupValues[1]` is `""` (not `null`) when only group 2 matches. Always `.takeIf { it.isNotBlank() }` on both groups in amount patterns.
- **KSP + clean**: Always `./gradlew clean assembleDebug` after adding new files. Never bare `assembleDebug`.
- **`rawSmsBody` on pre-2026-04-11 notification-path expenses**: null in DB (bug was fixed). Edit Expense infers auto-import from `sourceType` and shows "Auto-imported from SMS" label for those.
- **Claude API proxy (api.gameron.me)**: Uses `Authorization: Bearer <key>` header (not `x-api-key`). Model name is `claude-haiku-4.5` (not `claude-haiku-3-5`). Test with `curl` before assuming the model string is correct on a new proxy.
- **`local.properties` quoted values**: The `localProp()` helper in `data/build.gradle.kts` strips surrounding double-quotes, so `CLAUDE_API_KEY="sk-..."` and `CLAUDE_API_KEY=sk-...` both work.
- **Room migration index naming**: When writing `CREATE INDEX` in a migration, the name MUST match Room's auto-generated convention `index_<tableName>_<col1>_<col2>`, OR the `@Index` annotation on the entity must specify the same custom name explicitly. Mismatch causes `IllegalStateException: Migration didn't properly handle` crash on launch. See `.claude/skills/room-migration-gotchas.md`.
- **Dedup sourceType coverage**: Both `PendingNotificationManager` and `SmsImportViewModel` dedup checks must include `NOTIFICATION_AUTO` alongside `SMS_AUTO` — otherwise expenses confirmed from live notifications are invisible to subsequent dedup checks.
