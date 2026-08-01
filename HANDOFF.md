# Expense Analyst — Handoff

**Last updated**: 2026-06-14
**DB version**: 19
**Build**: `./gradlew clean assembleDebug` ✅
**Repo**: `https://github.com/psanup89-rgb/expense-analyst` (public)
**Release**: v0.6.1-debug (GitHub Release with APK)

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
