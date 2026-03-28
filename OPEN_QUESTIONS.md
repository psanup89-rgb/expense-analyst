# Expense Analyst — Open Questions

Items that could not be determined from the code alone, appear inconsistent, or require human clarification before work continues.

---

## 1. HANDOFF.md accuracy ✅ RESOLVED 2026-03-29

HANDOFF.md has been rewritten to reflect DB v10, Bills feature complete, Inbox restored, all 2026-03-29 session work. README.md still shows DB v9 (minor, not blocking).

---

## 2. Bills tab — bill detail screen

**Issue**: `BillsScreen.kt` shows a card with `BillCard` composable, but there is no `BillDetailScreen` registered in `AppNavGraph.kt`. Tapping a bill card has no navigation action wired (based on visible code).

**Question**: Is a Bill Detail screen planned? Should tapping a bill card open a detail view showing the linked payment expenses?

**Impact**: Bills tab is functional for viewing status but has no drill-down.

---

## 3. Duplicate detection for live notifications

**Issue**: `SmsReceiver` and `TransactionNotificationService` have no deduplication. The same SMS can create multiple inbox entries. Bulk import has two-tier deduplication; the live path does not.

**Question**: Should the live path deduplicate against (a) existing pending inbox items, (b) already-saved expenses, or (c) both?

**Impact**: Users with dual-SIM or SMS retry can get duplicate inbox entries.

---

## 4. Home currency hardcoded in Expense Detail

**Issue**: `ExpenseDetailScreen.kt` (line ~240) hardcodes `"SAR"` as the home currency check:
```kotlin
if (expense.currencyCode != "SAR") { // show if not same as home
```
This is not reading from `CurrencyRepository.getHomeCurrency()`.

**Question**: Is SAR intentionally hardcoded for the initial user base (Saudi Arabia), or is this a bug that should use the user's actual home currency setting?

**Impact**: Users who set INR or USD as home currency will see incorrect behaviour on the detail screen.

---

## 5. Settings screen — category management

**Issue**: Category management (add/edit/delete custom categories) is listed as a known gap in the settings screen. The pre-seeded categories are hardcoded in `ExpenseAnalystDatabase.kt`. `CategoryRepository` has a `getCategories()` method but no write operations are exposed.

**Question**: Is category management planned for Phase 1.5 or deferred to Phase 2?

**Impact**: Users cannot add a custom category (e.g. "Fuel") or rename "Other" to something more meaningful.

---

## 6. Parser registry — Mubasher position

**Issue**: `MubasherParser` was added to `ParserRegistry` but the plan noted it should go "before GenericParser, after D360Parser." The exact position matters because the Saudi-bank parsers (AlRajhi, StcBank, Alinma, D360) all come before Generic and could potentially conflict.

**Question**: Has the Mubasher sender pattern been verified not to overlap with any existing parser's `canParse()`?

**Impact**: If Mubasher SMS are sent from a sender matching another parser's pattern, the wrong parser runs first.

---

## 7. Bill statement parser — statement keyword false positives

**Issue**: `GenericStatementParser.canParse()` triggers on any SMS body containing `\bstatement\b` + `\btotal due\b` etc. Regular transaction SMS from some banks (e.g. HDFC Savings alerts) mention "statement" in passing.

**Question**: Has the Generic statement parser been tested against the full SMS corpus to verify it doesn't accidentally classify transaction SMS as bill statements?

**Impact**: Transaction SMS misrouted to `BillStatementManager` would create phantom Bill records.

---

## 8. Exchange rate — offline manual entry

**Issue**: When `CurrencyRepository.refreshRates()` fails (no internet) and the stored rate is stale, the app falls back to `SeedCurrencyRates`. There is no UI to let the user enter an exchange rate manually for an expense.

**Question**: Is offline manual rate entry needed, or is the seed-rate fallback acceptable for all intended use cases?

**Impact**: Users in areas with unreliable connectivity may get incorrect homeAmount calculations.

---

## 9. Release build / ProGuard

**Issue**: No ProGuard/R8 rules file exists in the codebase. `assembleRelease` has not been verified to produce a working APK (Hilt, Room, and Ktor all require specific keep rules).

**Question**: Is a signed release build needed in the near term? If so, R8 rules need to be written and tested.

**Impact**: Debug build works fine; release build is unverified.

---

## 10. Test device — Samsung S26 Ultra

**Issue**: The project uses a real device (Samsung Galaxy S26 Ultra, SM-S948B) for testing via ADB. The Android emulator is not mentioned anywhere.

**Question**: Is the emulator set up and usable as a fallback for testing? SMS permission flows behave differently on emulator vs real device.

**Impact**: Agents without access to the device cannot verify SMS capture features.
