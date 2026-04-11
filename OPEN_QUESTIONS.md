# Expense Analyst — Open Questions

Items requiring human input or that are ambiguous in the code.

---

## 1. Home currency hardcoded in Expense Detail ⚠️ BUG

**File**: `ExpenseDetailScreen.kt` ~line 240
```kotlin
if (expense.currencyCode != "SAR") { // should be uiState.homeCurrency
```
Users who set INR or USD as home currency see incorrect behaviour on the detail screen.

**Fix is straightforward** — see `HANDOFF.md` "First Action for Next Agent".

---

## 2. Release build / ProGuard

No ProGuard/R8 rules file exists. `assembleRelease` is unverified (Hilt, Room, and Ktor all require keep rules).

**Question**: Is a signed release build needed soon?

---

## 3. "Unknown Bank" auto-detected accounts

SMS-inferred accounts are created with `displayName = "Unknown Bank *XXXX · Savings"` when the bank name can't be parsed. There's no cleanup UX (user can edit account names in Manage Accounts, but they need to know to do it).

**Question**: Should account matching try harder to infer bank name from SMS sender? Or add a "Review Accounts" prompt after bulk SMS import?

---

## 4. Parser registry — Mubasher sender overlap

`MubasherParser` was added to `ParserRegistry` but its sender pattern hasn't been formally verified against the other Saudi bank parsers (AlRajhi, StcBank, Alinma, D360).

**Question**: Has the Mubasher `canParse()` been tested against the full SMS corpus?

---

## 5. Exchange rate — offline manual entry

When `refreshRates()` fails and the stored rate is stale, the app falls back to `SeedCurrencyRates`. No UI to enter an exchange rate manually.

**Question**: Is seed-rate fallback acceptable for all intended use cases?

---

## 6. `DuckDuckGoApiService.kt` dead code

`data/` module contains `DuckDuckGoApiService.kt` — unused since Google Places replaced it.

**Action**: Safe to delete. No callers.

---

## ✅ RESOLVED

- **Bill detail screen** (item 2, 2026-03-29): `BillDetailScreen` exists and is wired.
- **Duplicate detection for live notifications** (item 3): Soft-duplicate system shipped in DB v12.
- **JAVA_HOME not in shell profile** (item 11): Build works without manual workaround in current sessions.
- **APK signing mismatch** (item 12): No longer blocking — `installDebug` works on SM-S948B.
- **Category management** (item 5): Category management screen shipped (add/edit/delete/icon picker).
