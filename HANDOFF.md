# Expense Analyst — Handoff

**Last updated**: 2026-08-19
**DB version**: 20
**Build**: `./gradlew clean assembleDebug` ✅
**Repo**: `https://github.com/psanup89-rgb/expense-analyst` (public)
**Release**: v0.7.0-debug (GitHub Release with APK)

---

## Session Summary (2026-08-19) — "Add note" inline reply on the transaction notification

Auto-saved expenses always got `description = ""` and there was no way to record *what the spend was
for* without opening the app and editing. The notification now carries an inline `RemoteInput` reply
that writes the description straight from the shade.

### What shipped
- **`TransactionAlertNotification`**: `postForExpense` now attaches an "Add note" action. Added
  `postNoteSaved` / `postNoteFailed` / `repostForNoteRetry` / `cancel`, plus private
  `contentIntentFor` (shared by all posting paths so the `ACTION_OPEN_EXPENSE_DETAIL` route can't
  drift) and `buildNoteAction`. **Legacy `post()` and `postNotification()` are byte-identical** —
  verified by diff, so the stale-tray path is provably untouched.
- **`NoteReplyReceiver`** (new): `@AndroidEntryPoint` BroadcastReceiver, `goAsync()` for the write,
  `exported="false"`, no intent-filter (reached only via an explicit PendingIntent).
- **`NoteReplySanitizer`** (new): pure Kotlin — whitespace collapse, trim, 200-char cap that won't
  split a surrogate pair. Isolated precisely because it's the only genuinely unit-testable piece.
- **`ExpenseDao.updateDescription`** + `ExpenseRepository.updateDescription` + impl.
- No DB migration — `description` already existed in v20.
- 11 new tests (`NoteReplySanitizerTest` 7, `ReplyRequestCodeTest` 4), all passing.
- Version bumped to **0.7.0 / versionCode 2**. It had been stuck at `0.1.0` / `1` through every
  release since v0.1.5.

### Three decisions worth not re-litigating
1. **Targeted UPDATE, not `updateExpense`.** A full-row round-trip would null `account_number`
   (`ExpenseMapper.toEntity` hardcodes it), rewrite the tag join table, and race an open Edit Expense
   screen. `getExpenseByIdWithCategory` also has no `is_deleted` filter, so the guard belongs in SQL.
   The rows-affected return doubles as the receiver's success signal.
2. **`needsReview` is not cleared by a note.** `NeedsReviewEvaluator.evaluate()` doesn't take
   `description` as an input, so clearing it would desync the persisted flag from what the evaluator
   would compute, and would wrongly decrement the Review badge.
3. **Replace, not append.** The confirmation notification carries no reply action and self-dismisses,
   so there's no second-reply affordance. Re-editing happens in-app.

### Not yet verified on device
The build and unit tests are green, but **no on-device testing has run** — no device was attached
during the session. The two tests that would actually expose a defect:
- **Cold start**: post a notification, `adb shell am force-stop com.expenseanalyst`, then reply from
  the shade. Proves Hilt cold-start field injection and `goAsync()` process survival.
- **Multiple notifications**: fire three distinct test SMS, reply to the *middle* one, confirm the
  note lands on the right expense. Proves `replyRequestCode` isolation.

Drive both through the existing debug receiver:
```bash
adb shell am broadcast -a com.expenseanalyst.TEST_SMS --es sender "ALRAJHI" --es body "Your account has been debited SAR 150.00 At:Noon Ref:12345 Bal:SAR 5000.00"
```

### Pre-existing failure noticed (not caused here)
`:domain:test` → `CreateEmiFromExpenseUseCaseTest > invoke calculates correct installment with
interest` fails: expected `1064.65`, actual `1066.1854641401003`. Confirmed by stashing all changes
and re-running against pristine `HEAD`. The EMI interest formula and the test's expected value
disagree; **someone needs to decide which is right.** Untouched this session.

---

## Session Summary (2026-08-01) — Needs Review reasons (DB v20)

Needs Review cards now show *which* field(s) caused the flag, e.g. "Missing: Merchant, Account", instead of just being an unexplained flagged item.

- **Root cause investigated first**: the boolean `needsReview` flag was computed once at capture time (`PendingNotificationManager.kt`) from 4 conditions (blank merchant / generic category / unresolved payment method / no account last-4), but only the boolean was persisted — the reasons were discarded, so the list couldn't say why.
- **Correctness finding**: 3 of the 4 reasons (category, payment method, account) can be recomputed later from persisted fields, but the merchant reason cannot — `PendingNotificationManager` always backfills a blank merchant with the bank name before saving, so `expense.merchantName` is never blank in practice. Recomputing live would have silently hidden the most common trigger. Persisting the actual reasons at capture time was the only accurate option.
- **DB v20**: `needs_review_reasons` TEXT column on `expenses` (comma-separated `ReviewReason` enum names), `MIGRATION_19_20`.
- **New**: `domain/util/NeedsReviewEvaluator.kt` — `ReviewReason` enum + `evaluate()`/`encode()`/`decode()`, single source of truth used by both `PendingNotificationManager` (write) and `NeedsReviewScreen` (display, via `Expense.reviewReasons`, which is only ever decoded from what was persisted, never re-evaluated).
- Added `Expense.accountLastFour` (from the already-joined `AccountEntity` relation) and `Expense.reviewReasons: List<ReviewReason>`.
- New test: `domain/src/test/.../util/NeedsReviewEvaluatorTest.kt`.

---

## Session Summary (2026-06-14) — Docs sync (DB v19) + Gradle/AGP/Kotlin/Room bump + release v0.6.1

### 1. Docs brought up to date with DB v19 (auto-save + Needs Review, commit `8be2fa3`)

`STATUS.md`, `HANDOFF.md`, `PROJECT.md`, `CONSTRAINTS.md`, `AGENTS.md`, `CLAUDE.md`, `docs/FEATURES.md`, `docs/DATA_MODELS.md` were all still describing DB v18 and the old tap-to-save Pending Inbox flow. Updated to reflect:
- DB v19 (`needs_review` column on `expenses`), next migration is `MIGRATION_19_20`
- Bottom nav is now **Home · Review · Bills · EMI · Settings** (5 destinations, not 3 — `CLAUDE.md` was wrong)
- Old Pending Inbox is repurposed as "Pending Bill Statements" (BILL type only, reached via Bills screen)
- `lent_items` table (DB v18, previously undocumented) added to `docs/DATA_MODELS.md`

### 2. Build tooling bump (no app-facing change)

`gradle/libs.versions.toml`: AGP 9.1.0 → 9.2.1, Kotlin 2.1.0 → 2.2.10, KSP 2.1.0-1.0.29 → 2.3.2, Room 2.7.0 → 2.7.2. `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.3.1 → 9.4.1. New `gradle/gradle-daemon-jvm.properties` (Gradle's daemon toolchain pin, auto-generated).

### 3. Release v0.6.1 published

Debug APK (`app-debug.apk`) built via `./gradlew clean assembleDebug` and attached to GitHub Release `v0.6.1-debug`, following the established convention (no release-signing keystore exists yet — see Open Question in `NEXT_STEPS.md`).

---

## Session Summary (2026-06-13) — Issue #15 fix + build environment setup

### 1. SaudiEnergyStatementParser — unpaid-reminder SMS misclassified as expense (#15)

SMS: `"We would like to remind you that your issued bill for account No. 30166041401 in the amount of 82.92 SAR has not been paid."`

| Bug | Fix |
|-----|-----|
| Fingerprint `your\s+bill\s+for\s+account` didn't match "your **issued** bill for account" | Made `issued` optional: `your\s+(?:issued\s+)?bill\s+for\s+account\b` |
| Account pattern `account\s+(\d+)` skipped "No." prefix → account number not extracted | Added optional `(?:[Nn]o\.?\s+)?` before digits |
| `GenericParser.billReminderPattern` had no guard for "has not been paid" phrasing | Added `(?:issued\s+)?bill.{0,80}has\s+not\s+been\s+paid` as defense-in-depth |

New `SaudiEnergyStatementParserTest` (7 tests). New test in `GenericParserTest` verifying null result for the exact issue #15 SMS.

**Files changed**: `SaudiEnergyStatementParser.kt`, `GenericParser.kt`, `SaudiEnergyStatementParserTest.kt` (new), `GenericParserTest.kt`

### 2. GitHub issues closed

Issues #10 (Loans), #14 (GenericParser CC auth), #15 (SaudiEnergy) closed with explanatory comments. **No open issues remain.**

### 3. Build environment (first-time Mac setup)

- JDK 21 required (project targets `JavaVersion.VERSION_21`). Installed via `brew install --cask temurin@21`
- `local.properties` created: `sdk.dir=/Users/anoop-ksa0043/Library/Android/sdk`
- Build env: `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`, `ANDROID_HOME=/Users/anoop-ksa0043/Library/Android/sdk`
- APK installed on device `192.168.100.113` (wireless ADB — owner's device)

---

## Session Summary (2026-06-06) — Loan/Lent tracking (#10) + GenericParser fix (#14)

### 1. GenericParser — CC auth SMS not detected (#14)

Three bugs fixed: `authorized` added to weak-debit keywords; `*` added to merchant char class with `\s+on\s+\d` stop; currency detection now scans amount-match text first.

**Files changed**: `GenericParser.kt`, `GenericParserTest.kt`

### 2. Loans/Lent tracking feature (#10) — new `:feature:loans` module

DB v18: `lent_items` table (16 columns). Domain: `LentItem.kt`, `LentRepository.kt`. Data: entity + DAO + mapper + impl. WorkManager reminders via `LentReminderWorker` + `LentReminderScheduler`. UI: `LoanListScreen`, `AddLoanScreen`, `LoanDetailScreen`. Settlement creates INCOME+Refund expense to net out of monthly totals. Entry point: Settings → "Loans & Lending".

---

## Session Summary (2026-05-11) — Bug sweep: Issues #4–#9, #11, #12

- **AxisBankStatementParser** (#4, #7): fingerprint + due-date pattern extended for newer CC reminder format
- **KeetaParser** (#5, #8): refund/cancellation regex + canParse `[Keeta]` prefix handling
- **EmiratesNbdParser** (#6, #9): fingerprint widened for generic short-code SMS + POS Reversal
- **PAYMENT routing** (#6): `PendingNotificationManager` sets merchant = "BillPayments" for PAYMENT with blank merchant; auto-links to open bill via `BillMatcher`
- **BillMatcher** (#11): strict amount-tolerance matching (±5% or minimumDue); replaces loose substring match
- **Refunds reduce monthly Spent** (#12): `ExpenseListViewModel` + `AnalyticsViewModel` subtract INCOME+Refund from gross totals

---

## Open Issues

**None.** All GitHub issues resolved.

---

## First Action for Next Agent

Priority order:

0. **Verify the "Add note" reply on a real device** (see 2026-08-19 session summary) — cold start and
   multi-notification cases especially. And decide whether the EMI interest formula or its test
   expectation is the wrong one.
1. **CSV/PDF export (F14)** — most-requested Phase 2 feature. Export filtered expense list as CSV; PDF is a stretch goal.
2. **Home screen widget (F16)** — monthly spend summary widget using Glance API.
3. **ProGuard release smoke-test** — `./gradlew assembleRelease` needs a signing config (`keystore.properties` + signing block in `app/build.gradle.kts`). `proguard-rules.pro` already exists.

---

## Gotchas / Surprises

- **JDK 21 required** — project targets `JavaVersion.VERSION_21`. JDK 17 fails with `invalid source release: 21`.
- **Always `./gradlew clean assembleDebug`** — never bare `assembleDebug`. KSP incremental is disabled.
- **`LazyColumn` in AlertDialog**: use `Column + verticalScroll` instead.
- **`kotlinx-datetime` classpath**: `:feature` modules using `Expense.date` (type `Instant`) need `implementation(libs.kotlinx.datetime)` explicitly.
- **Two-group regex**: `groupValues[1]` is `""` not `null` when only group 2 matches. Always `.takeIf { it.isNotBlank() }`.
- **Dedup sourceType coverage**: Both `PendingNotificationManager` and `SmsImportViewModel` dedup checks must include `NOTIFICATION_AUTO` alongside `SMS_AUTO`.
- **Room migration index naming**: `CREATE INDEX` names must match Room's convention `index_<tableName>_<col1>` or the `@Index` annotation must use the exact same custom name.
- **Claude API proxy**: Uses `Authorization: Bearer <key>` header (not `x-api-key`). Model: `claude-haiku-4.5`.
- **RemoteInput needs a MUTABLE PendingIntent** — with `FLAG_IMMUTABLE`, `getResultsFromIntent()` silently returns null. Use `PendingIntentCompat`, never the bare API-31 `FLAG_MUTABLE` constant.
- **A RemoteInput reply leaves a progress spinner** on the notification until the app re-notifies the same id or cancels it. Every branch of a reply receiver must end in a notify or cancel, or it hangs there forever.
- **`updateExpense` is lossy**: `ExpenseMapper.toEntity` hardcodes `accountNumber = null`, so any full-row round-trip drops `account_number`. It also rewrites the tag join table. Prefer a targeted `@Query` UPDATE for single-field writes.
- **`getExpenseByIdWithCategory` has no `is_deleted = 0` filter** — unlike every other query in that DAO. Guard soft-deletes yourself.
