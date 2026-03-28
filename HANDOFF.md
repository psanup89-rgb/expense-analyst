# Expense Analyst — Handoff Document

**Last updated**: 2026-03-29
**DB version**: 10
**Build status**: `./gradlew clean assembleDebug` ✅ passing
**Device tested**: Samsung Galaxy S26 Ultra (SM-S948B), connected via ADB
**Branch**: `main` (all work pushed)

---

## What Was Built This Session (2026-03-28 → 2026-03-29)

### 1. Bills Tracking Feature (DB v9→v10) — commit `9516af4`

Full bill lifecycle: statement SMS → PENDING Bill → payment linked → SETTLED.

**New files created:**
- `domain/model/Bill.kt` — data class with billerName, totalDue, minimumDue, dueDateMillis, status (PENDING/PARTIAL/SETTLED)
- `domain/repository/BillRepository.kt` — interface with `findOpenBillByBiller(billerName, accountId)`
- `data/local/entity/BillEntity.kt` — Room entity for `bills` table
- `data/local/dao/BillDao.kt` — queries including `findOpenByBiller()` filtering PENDING + PARTIAL
- `data/repository/BillRepositoryImpl.kt`
- `feature/notification/parser/ParsedBillStatement.kt` + `BillStatementParser.kt` interface
- `feature/notification/parser/BillStatementParserRegistry.kt` — 4 parsers tried in order
- `feature/notification/parser/EmiratesNbdStatementParser.kt` — sender `enbd` + body `statement`
- `feature/notification/parser/AlRajhiStatementParser.kt` — sender `rajhi/74100` + body
- `feature/notification/parser/HdfcStatementParser.kt` — sender `hdfc` + `total amt due` format
- `feature/notification/parser/GenericStatementParser.kt` — strict fallback (body must have `statement` + `total due`)
- `feature/notification/service/BillStatementManager.kt` — `@Singleton`, creates/updates Bill from parsed statement
- `feature/expenses/ui/BillsUiState.kt`, `BillsViewModel.kt`, `BillsScreen.kt`
- `feature/notification/parser/MubasherParser.kt` — body fingerprint, always PAYMENT direction
- `feature/notification/parser/PaymentMethodDetector.kt` — centralised payment method inference
- `feature/notification/parser/EmiratesNbdParser.kt`, `IdfcFirstBankParser.kt`, `FasTagParser.kt`, `OneCardParser.kt`

**Files modified:**
- `domain/model/Enums.kt` — added `BillStatus { PENDING, PARTIAL, SETTLED }`
- `domain/model/Expense.kt` — added `billId: Long? = null`
- `domain/repository/ExpenseRepository.kt` — added `getExpensesByBillId(billId)`
- `data/local/ExpenseAnalystDatabase.kt` — bumped to v10, added MIGRATION_9_10 (bills table + expense.bill_id), added BillEntity, BillDao
- `data/local/entity/ExpenseEntity.kt` — added `bill_id` column
- `data/local/dao/ExpenseDao.kt` — added `getExpensesByBillId` query
- `data/mapper/ExpenseMapper.kt` — maps `billId` in both directions
- `data/repository/ExpenseRepositoryImpl.kt` — implements `getExpensesByBillId`
- `data/di/DatabaseModule.kt` — provides `BillDao`
- `data/di/RepositoryModule.kt` — binds `BillRepository`
- `feature/notification/parser/ParserRegistry.kt` — added MubasherParser before GenericParser
- `feature/notification/service/SmsReceiver.kt` — fallback to `BillStatementParserRegistry` when transaction parse fails
- `feature/notification/service/TransactionNotificationService.kt` — same fallback
- `feature/expenses/ui/AddExpenseViewModel.kt` — on PAYMENT save: finds open bill → links expense.billId → updates bill status
- `core/navigation/NavRoutes.kt` — added `BILLS = "bills"`
- `app/navigation/AppNavGraph.kt` — registered `BillsScreen`
- `app/ui/MainBottomNav.kt` — Home · Bills · EMI · Settings

**Build issue fixed during this work**: KSP reported "cannot resolve BillRepository" — root cause was Kotlin smart cast on cross-module nullable properties (`openBill.totalDue`, `bill.dueDateMillis`). Fix: extract to local `val` before null comparison. See `skills/ksp-cross-module-smart-cast.md`.

### 2. Bill Statement Parsing in Bulk Import — commit `a97f4d0`

`SmsImportViewModel.startBulkImport()` now tries `BillStatementParserRegistry` when `ParserRegistry.parse()` returns null. Import result screen shows "Bills detected: N". `BillStatementManager` injected into `SmsImportViewModel`.

### 3. Manual Bill Linking from Expense Detail — commit `c217c98`

PAYMENT expenses in detail screen now show:
- "Linked Bill: [name]" row if already linked
- "Link to Bill" button → `ModalBottomSheet` listing open bills if not linked
- "No open bills" (disabled) if none exist
`ExpenseDetailViewModel` extended with `BillRepository`, `ExpenseRepository`, open bills flow, `linkToBill()`.

### 4. Inbox Tab Restored — commit `3fe3c7d`

Bottom nav was accidentally set to Home · Bills · EMI · Settings when Bills was added. Fixed back to Home · Inbox (badged) · Bills · EMI · Settings. Added `NavRoutes.PENDING_INBOX` back to `showBottomNav` in `MainActivity`.

### 5. Inbox Dismiss Confirmation — commit `710300f`

Both dismiss paths in `PendingInboxScreen` now confirm first:
- Single dismiss: "This transaction has not been added to your expenses yet. Are you sure?"
- Clear All: shows count of items that would be lost
`PendingInboxViewModel` refactored to `requestDismiss()` / `confirmDismiss()` / `cancelDismiss()` pattern.

### 6. Project Memory System — no commit yet (part of session wrap-up)

Created at project root: `PROJECT.md`, `STATUS.md`, `AGENTS.md`, `CHANGELOG.md`, `OPEN_QUESTIONS.md`, `CONSTRAINTS.md`, `skills/`.

---

## What Was NOT Finished

| Item | Reason |
|------|--------|
| Live notification dedup | Not in scope this session — identified as recommended next task |
| Bill detail drill-down screen | Not requested — bills tab shows list only, no tap navigation |
| Parser tests for new parsers | Not in scope — MubasherParser, EmiratesNbdParser etc have no unit tests yet |
| HANDOFF.md was stale going into this session | It said DB v9 and missing Bills feature — now corrected (this file) |
| README.md DB version | Still says v9 — minor, not critical |

---

## First Action for Next Agent

Read `STATUS.md` → implement **live notification dedup** in `PendingNotificationManager.enqueue()`.

Exact scope:
1. Add `findByBodyHash(hash: Int): PendingNotification?` to `PendingNotificationRepository` interface and `PendingNotificationDao`
2. In `PendingNotificationManager.enqueue(parsed)`: compute `parsed.rawBody?.trim()?.hashCode()`, query for recent match (last 60s), skip if found
3. Also check `ExpenseRepository.getExpensesSnapshot()` for same hash in SMS_AUTO expenses — skip if already saved

No DB migration needed. Self-contained in `feature/notification` + `domain`.

---

## Gotchas and Non-Obvious Things

1. **KSP smart cast bug**: Cross-module nullable property checks (e.g. `if (openBill.totalDue == null || paid >= openBill.totalDue)`) fail to compile with "smart cast impossible". Always extract to `val billTotalDue = openBill.totalDue` first. See `skills/ksp-cross-module-smart-cast.md`.

2. **KSP stale state**: Always `./gradlew clean assembleDebug`, never just `assembleDebug`. This applies especially after: adding new Hilt-injected classes, adding new Room entities, or adding new DAO methods.

3. **Bottom nav + showBottomNav must match**: When adding a new bottom nav destination, update BOTH `MainBottomNav.kt` items list AND the `showBottomNav` list in `MainActivity.kt`. Forgetting one causes the nav bar to disappear or show on wrong screens.

4. **5-tab bottom nav is Material 3 maximum**: Current nav is at the limit (Home · Inbox · Bills · EMI · Settings). Any new top-level destination must replace an existing one or live as a nested route.

5. **Bill statement parser false-positive risk**: `GenericStatementParser` is deliberately strict. If loosening its `canParse()` regex, test against the full SMS corpus — regular transaction SMS from some banks mention "statement" in passing.

6. **MubasherParser uses body fingerprint**: Sender from Mubasher is a numeric shortcode, not a recognisable name. Detection is based on body content (`Reason:.*Bills Payment` or `Amount:SAR`). See `skills/parser-body-fingerprint.md` for the full pattern.

7. **`combine()` with 5+ flows**: `uiState` in several ViewModels uses `combine()` with 5 flows. Kotlin's `combine` is overloaded up to 5 parameters — beyond 5, use nested `combine()` calls.

8. **Device is S26 Ultra (SM-S948B)**: The `adb devices` display name says "SM-S948B - 16". Do not infer the marketing name from this — trust the user's stated device name.

---

## Handoff Checklist for Next Agent

1. Read `CLAUDE.md` (conventions, architecture, build commands)
2. Read this file and `STATUS.md`
3. Run `./gradlew clean assembleDebug` — confirm it passes
4. DB is at **v10** — next migration is `MIGRATION_10_11`
5. Check `skills/` directory for relevant patterns before starting
6. Update `STATUS.md` and this file at end of session
