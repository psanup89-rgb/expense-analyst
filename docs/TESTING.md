# Testing Strategy

## Overview
Testing follows the testing pyramid: many unit tests, moderate integration tests, fewer UI/E2E tests.

**Coverage target**: 80%+ line coverage on `:domain` and `:data` modules.

Current repository status: the dependencies and testing strategy are documented here, but test source sets have not been added yet in the current workspace. Treat this document as the target testing plan to build out alongside upcoming phases.

Current manual regression priority:
- Add Expense save flow
- Home list grouping and totals
- Home-currency change in Settings
- Multi-currency conversion display and repair behavior

## Test Stack

| Tool | Purpose |
|------|---------|
| JUnit 5 | Unit test framework |
| MockK | Kotlin mocking library |
| Turbine | Kotlin Flow testing |
| Room Testing | In-memory database for DAO tests |
| Compose UI Test | Jetpack Compose UI testing |
| Ktor Mock Engine | Mock HTTP responses for API tests |
| Robolectric | Android framework classes in JVM tests |

## Test Organization

```
module/src/test/          → Unit tests (JVM, fast)
module/src/androidTest/   → Instrumented tests (emulator, slower)
module/src/test/resources/ → Test fixtures (CSV samples, JSON responses)
```

## What to Test by Layer

### Domain Layer (`:domain`) — Unit Tests

**Use Cases** — Most important tests:
```
AddExpenseUseCase:
  ✓ Delegates to repository and returns the inserted row ID (Long)
  ✓ Passes the expense model through unchanged

Note: AddExpenseUseCase has no validation logic — it's a thin delegate.
Validation lives in AddExpenseUiState.isValid (ViewModel layer).

CreateEmiFromExpenseUseCase:
  ✓ 6 months, no interest → 6 equal installments
  ✓ 12 months, 12% interest → correct EMI calculation
  ✓ Installment dates are 1 month apart
  ✓ First installment date matches start date
  ✓ All installments linked to same emiGroupId
  ✓ Months < 2 returns Error
  ✓ Months > 60 returns Error

ConvertCurrencyUseCase:
  ✓ Same currency returns original amount
  ✓ Different currency applies correct rate
  ✓ Missing rate returns Error
  ✓ Handles zero amount
```

### Data Layer (`:data`) — Unit + Integration Tests

**DAOs** (integration, in-memory Room DB):
```
ExpenseDao:
  ✓ Insert and retrieve expense
  ✓ getExpensesByDateRange returns correct range
  ✓ getExpensesByCategory filters correctly
  ✓ softDelete sets isDeleted = 1
  ✓ getAllExpenses excludes soft-deleted
  ✓ getCategoryTotals aggregates correctly
  ✓ getEmiInstallments returns ordered by number
  ✓ Flow emits on insert/update/delete

CategoryDao:
  ✓ Pre-seeded categories exist after DB creation
  ✓ Insert custom category
  ✓ Cannot insert duplicate name

EmiGroupDao:
  ✓ Insert group and retrieve
  ✓ Delete group cascades (or not — test the behavior)
```

**Repositories** (unit tests with mocked DAOs):
```
ExpenseRepositoryImpl:
  ✓ getExpenses maps entities to domain models
  ✓ addExpense maps domain to entity and inserts
  ✓ Uses correct mapper

CurrencyRepositoryImpl:
  ✓ Returns cached rate if fresh (< 24h)
  ✓ Fetches new rates if stale (> 24h)
  ✓ Returns cached rate if API fails (offline)
  ✓ Saves fetched rates to cache
```

**Mappers** (unit tests):
```
ExpenseMapper:
  ✓ Entity → Domain preserves all fields
  ✓ Domain → Entity preserves all fields
  ✓ Handles null optionals correctly
  ✓ Date conversion: millis ↔ Instant
```

### Notification Parsers — Parameterized Unit Tests

**This is the highest-risk component. Test extensively.**

Each bank parser gets its own test class with parameterized tests:

```kotlin
@ParameterizedTest
@CsvFileSource(resources = ["/sms_samples/hdfc.csv"], numLinesToSkip = 1)
fun `parse HDFC SMS`(
    sender: String,
    messageBody: String,
    expectedAmount: Double,
    expectedType: String,
    expectedMerchant: String?,
    expectedCurrency: String,
    expectedAccountLast4: String?
) {
    val result = hdfcParser.parse(messageBody)
    assertNotNull(result)
    assertEquals(expectedAmount, result.amount, 0.01)
    assertEquals(expectedType, result.transactionType.name)
    // ... etc
}
```

**Test fixtures**: `feature/notification/src/test/resources/sms_samples/`
- `hdfc.csv`, `sbi.csv`, `icici.csv`, `axis.csv`, `kotak.csv`, `yesbank.csv`
- `alrajhi.csv`, `stcbank.csv`, `alinma.csv`, `d360.csv`
- `googlepay.csv`, `phonepe.csv`, `paytm.csv`
- `applepay.csv`, `googlewallet.csv`, `samsungpay.csv`

**Target**: 95%+ parse success rate per supported bank.

### Core Utilities — Unit Tests

```
DateTimeUtil:
  ✓ UTC millis → local date string (various timezones)
  ✓ "Today" / "Yesterday" labels correct in local timezone
  ✓ Start/end of day in UTC for date range queries
  ✓ Add months for EMI date generation

CurrencyFormatter:
  ✓ INR: "₹1,23,456.78" (Indian grouping)
  ✓ USD: "$1,234.56"
  ✓ SAR: "SAR 1,234.56"
  ✓ Handles zero, negative, large amounts
```

### UI Tests — Compose Instrumented Tests

```
ExpenseListScreen:
  ✓ Displays expenses grouped by date
  ✓ Filter chips filter the list
  ✓ FAB navigates to Add screen
  ✓ Swipe to delete shows undo snackbar
  ✓ Empty state shown when no expenses

AddExpenseScreen:
  ✓ Amount input accepts valid numbers
  ✓ Rejects empty amount on save
  ✓ Category selection highlights selected
  ✓ Save creates expense and navigates back
  ✓ Currency picker opens and selects

EditExpenseScreen:
  ✓ Pre-fills all fields with existing values
  ✓ Save updates expense
  ✓ Cancel discards changes

EmiConversionSheet:
  ✓ Shows correct installment preview
  ✓ Updates preview when months change
  ✓ Interest rate changes installment amount
  ✓ Confirm creates EMI group
```

## Running Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific module
./gradlew :domain:test
./gradlew :data:testDebugUnitTest

# Instrumented tests (requires emulator)
./gradlew connectedDebugAndroidTest

# Code quality
./gradlew ktlintCheck
./gradlew detekt

# Coverage report
./gradlew jacocoTestReport
# Output: build/reports/jacoco/

# All checks
./gradlew check
```

## CI Pipeline (GitHub Actions)

```yaml
name: CI
on: [push, pull_request]
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - run: ./gradlew ktlintCheck detekt
      - run: ./gradlew testDebugUnitTest
      - run: ./gradlew assembleRelease
      - uses: actions/upload-artifact@v4
        with:
          name: release-apk
          path: app/build/outputs/apk/release/*.apk
```

## E2E Test Scenarios

1. **Full manual flow**: Onboard → Add expense → View in list → Edit → Verify
2. **Multi-currency**: Add USD expense with INR home → verify conversion displayed
3. **EMI flow**: Add expense → Convert to 6-month EMI → Verify 6 installments → Cancel remaining
4. **Notification**: Simulate SMS → Confirm banner → Expense saved correctly
5. **Offline**: Disable network → Add expense → Re-enable → Rates sync

## Current Manual Smoke Checklist
1. Open Home and confirm monthly/day totals load.
2. Add one home-currency expense and confirm it appears immediately.
3. Add one foreign-currency expense and confirm the row shows both original and home amounts.
4. Open Settings and change home currency; verify Home totals and row-level home amounts refresh correctly.
5. Relaunch the app and verify the repaired conversion values persist.
