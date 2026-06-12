# Expense Analyst — Open Questions

Items requiring human input or that are ambiguous in the code.

---

## 1. Home currency hardcoded in Expense Detail ⚠️ BUG

**File**: `ExpenseDetailScreen.kt` ~line 240
```kotlin
if (expense.currencyCode != "SAR") { // should be uiState.homeCurrency
```
Users with INR or USD as home currency see incorrect behaviour on the detail screen.
**Fix is straightforward** — pass `homeCurrency` from ViewModel state.

---

## 2. "Unknown Bank" auto-detected accounts

SMS-inferred accounts are created with `displayName = "Unknown Bank *XXXX · Savings"` when the bank name can't be parsed. User can rename via Manage Accounts but there's no prompt to do so.

**Question**: Add a "Review Accounts" prompt after bulk SMS import?

---

## 3. Parser registry — Mubasher sender overlap

`MubasherParser` sender pattern hasn't been formally tested against the full Saudi bank parser corpus (AlRajhi, StcBank, Alinma, D360).

**Question**: Has `MubasherParser.canParse()` been tested against the full SMS corpus?

---

## 4. Exchange rate — offline manual entry

When `refreshRates()` fails and stored rate is stale, app falls back to `SeedCurrencyRates`. No UI to enter a rate manually.

**Question**: Is seed-rate fallback acceptable for all intended use cases?

---

## ✅ RESOLVED

- **Bill detail screen**: `BillDetailScreen` exists and is wired.
- **Duplicate detection for live notifications**: Shipped in DB v12.
- **Category management**: Add/edit/delete/icon picker screen shipped.
- **ProGuard rules**: `app/proguard-rules.pro` created with full rules for Kotlin/Hilt/Room/Ktor.
- **`DuckDuckGoApiService.kt` dead code**: Already absent from codebase — never committed or deleted in prior session.
- **APK signing mismatch**: Resolved — debug APK installed on device via `adb install -r`.
