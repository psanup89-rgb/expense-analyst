# Expense Analyst — Handoff Document

**Last updated**: 2026-03-29
**DB version**: 10 (confirmed in `ExpenseAnalystDatabase.kt`)
**Build status**: `./gradlew clean assembleDebug` ✅ passing (verified this session)
**Device**: Samsung Galaxy S26 Ultra (SM-S948B), ADB connected — APK NOT YET INSTALLED (signing key mismatch, see blockers)
**Branch**: `main` (all work pushed)

---

## What Was Built This Session (2026-03-29)

### 1. STATUS.md Corrections
- DB version corrected from v9 → v10 (STATUS.md was stale)
- Live notification dedup marked ✅ Done (was already fully implemented — DAO + repository + `PendingNotificationManager.enqueue()`)
- Recommended next task updated to reflect actual remaining work

### 2. Al Rajhi "Credit Transfer Internal" Parser Fix

**Problem**: SMS like:
```
Credit Transfer Internal
Amount:SAR 5000
To:6805
From:MOHAMATHU PILLAI
From:5119
26/3/29 14:05
```
was being misdetected as a **Mubasher payment** instead of an Al Rajhi internal transfer.

**Root cause (two bugs)**:
1. `AlRajhiParser.parse()` guarded on `isDebit || isCredit` keywords — "Credit Transfer Internal" matched neither → returned null
2. `MubasherParser.bodyFingerprintPattern` had a second branch `Amount\s*:\s*SAR\s*\d` that matched ANY SAR amount SMS — fired as false fallback

**Files changed**:
- `ParsedTransaction.kt` — added `TRANSFER` to `TransactionDirection` enum
- `AlRajhiParser.kt` — added `transferFingerprintPattern`, updated `canParse()`, added transfer branch in `parse()` that extracts `To:XXXX` → accountLast4, `From:NAME` → merchant, hardcodes `NET_BANKING`
- `MubasherParser.kt` — narrowed body fingerprint: `Amount:SAR \d` replaced with `(?:Biller|Service)\s*:` (Mubasher-specific fields only)
- `SmsImportViewModel.kt` — added `TRANSFER → TransactionType.TRANSFER` mapping
- `TransactionAlertNotification.kt` — added "Transfer" label for TRANSFER direction
- `NotificationBanner.kt` — added "transferred" label for TRANSFER direction
- `AlRajhiParserTest.kt` — added transfer parse test + canParse fingerprint test
- `MubasherParserTest.kt` — added non-match test for transfer body, added Service field test

**Parsed result for the sample SMS**:
- Type: TRANSFER ✅
- Account last4: 6805 ✅
- Merchant: MOHAMATHU PILLAI ✅
- Payment Method: NET_BANKING ✅
- Currency: SAR ✅

### 3. In-App Banner Not Dismissing After Tray-Tap → Save Flow

**Problem**: When user taps the system tray notification → goes to `AddExpenseScreen` → saves → returns to Home, the in-app banner still showed.

**Root cause**: The in-app banner's `onSave` calls `PendingNotificationManager.consume()` to clear `_pending`. But the tray notification tap path navigates directly to `AddExpenseScreen` without going through the banner — `consume()` never fires. `_pending` remains set.

**Fix**:
- `MainViewModel.kt` — added `fun dismissBanner() { pendingManager.dismiss() }`
- `AppNavGraph.kt` — `ADD_EXPENSE_ROUTE` `onSaved` now calls `mainViewModel?.dismissBanner()` before `popBackStack()`

Correct for all paths: tray tap (dismisses on save), in-app banner tap (consume already cleared it, dismiss is no-op), manual add (no pending, dismiss is no-op), back without saving (onSaved not called, banner stays).

---

## What Was NOT Finished

| Item | Reason |
|------|--------|
| APK installation on device | Signing key mismatch — existing app signed with different key. User must uninstall manually from device settings, then run `adb install`. |
| Home currency hardcoded in ExpenseDetail | Identified as next task, not in scope this session |
| ProGuard/R8 rules | Not in scope |

---

## First Action for Next Agent

1. Set `JAVA_HOME` for build:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ```
2. Read `STATUS.md` → implement **home currency fix in `ExpenseDetailScreen`** (~line 240)
   - Replace `!= "SAR"` with `!= uiState.homeCurrency`
   - Add `homeCurrency: String` to `ExpenseDetailUiState`
   - Collect from `CurrencyRepository.getHomeCurrency()` in `ExpenseDetailViewModel`

---

## Gotchas and Non-Obvious Things

1. **MubasherParser false-match pattern**: The second branch of `bodyFingerprintPattern` was `Amount\s*:\s*SAR\s*\d` — matches everything. Always require at least one service-specific field in Mubasher fingerprint. See `skills/parser-mubasher-fingerprint.md`.

2. **Al Rajhi internal transfers have two `From:` lines**: `From:MOHAMATHU PILLAI` (name) and `From:5119` (account). Use `[A-Za-z][A-Za-z ]{2,}` pattern to match names only — digit-form `From:` won't match.

3. **TRANSFER direction checklist**: Adding a new `TransactionDirection` value requires updates to: `SmsImportViewModel` (when block), `TransactionAlertNotification` (label), `NotificationBanner` (label). `SmsImportScreen` icon tint uses if/else so TRANSFER falls to else (acceptable). See `skills/transaction-direction-enum-extension.md`.

4. **Banner dismiss must be in AppNavGraph `onSaved`**: `AddExpenseViewModel` is in `:feature:expenses` and cannot import `PendingNotificationManager` from `:feature:notification`. The dismiss must go through `MainViewModel` (in `:app`) which can reach the manager. See `skills/banner-dismiss-on-save.md`.

5. **Gradle JAVA_HOME**: Shell profile does not set `JAVA_HOME`. Build fails with "Unable to locate a Java Runtime". Always set `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` before running Gradle commands.

6. **APK signing mismatch**: Installing a debug APK over an existing release/differently-signed APK fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. User must uninstall the old app first (loses local data). Warn the user before doing this.

---

## Handoff Checklist for Next Agent

1. Read `CLAUDE.md` (conventions, architecture, build commands)
2. Read this file and `STATUS.md`
3. Set `JAVA_HOME` (see above)
4. Run `./gradlew clean assembleDebug` — confirm it passes
5. DB is at **v10** — next migration is `MIGRATION_10_11`
6. Check `skills/` directory for relevant patterns before starting
7. Update `STATUS.md` and this file at end of session
