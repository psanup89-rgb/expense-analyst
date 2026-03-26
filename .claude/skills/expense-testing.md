# Expense Testing Skill

You are a specialist in setting up, maintaining, and ensuring test coverage for the **Expense Analyst** Android app.

## Overview
The testing architecture strictly enforces an 80%+ line coverage across the `:domain` and `:data` modules.
The tests follow the testing pyramid. 
- High unit test count (JVM level). 
- Moderate Integration (Database / DAOs). 
- Less E2E UI mapping.

## The Testing Toolbox
1. **JUnit 5**: Standard unit testing.
2. **MockK**: Used extensively for Kotlin classes and interfaces.
3. **Turbine**: Facilitates testing operations emitting `Flow` outputs.
4. **Room Testing**: Memory Database implementation mimicking DAO logic.
5. **Ktor Mock Engine**: Fleshing out mock HTTP implementations directly inside `data` tests.

## Test Boundaries per Layer

### 1. Domain Layer (`:domain` / `Use Cases`)
Tests here must remain completely isolated from Android-related dependencies. Use Kotlin pure structures.
**Important verification tests:**
- `AddExpenseUseCase`: delegates to repository and returns the inserted row ID (`Long`). No validation logic in this use case — validation is in `AddExpenseUiState.isValid`.
- `CreateEmiFromExpenseUseCase`: correct installment count, date spacing, EMI math with/without interest.
- `CurrencyConversion`: same-currency passthrough, cross-currency math, stale-rate detection, missing-rate fallback.
- `GetExpensesUseCase` / `SoftDeleteExpenseUseCase`: correct delegation, no side effects.

### 2. Data Layer (`:data` / `DAOs` / `Repositories`)
Test DAO mappings with Integration memory Room databases.
**Important DAO behavior to emulate and verify:**
- Complete softDelete queries. Assert that records are flagged with `isDeleted = 1` inside Room instead of standard deletes.
- Retrieve only active documents unless explicitly querying deleted entities.
- Verify that `ExpenseRepositoryImpl` properly implements `ExpenseMapper` when handling entity transformations to `DomainModels` and vice-versa.
- Emitted variables inside Room flows.
- Offline-Caching validation for the `CurrencyRepositoryImpl` logic (rates returned successfully even failing online network requests).

### 3. Notification Parsers
This logic represents the riskiest architectural piece. Extensively parameterize all mock parsing strategies.
Uses `@CsvFileSource(resources = ["/sms_samples/XXX.csv"], numLinesToSkip = 1)`.
Every parser created must encompass testing against various fixture records simulating successful parsed components, fallback failure detection on undefined data strings, or format manipulation errors.
**Target Standard:** 95%+ success rate across bank samples.

### 4. Utilities
Ensure that formatting tools (`CurrencyFormatter`, `DateTimeUtil`) pass robust test coverage handling variations between edge cases (Zero, negatives, localized formatting strings for USD, SAR, INR, extremely large numbers, and UTC to local date transformations considering overlapping time zones).

## Commands for Manual Verification
Ensure that local verification works utilizing `gradle`:
1. **Unit tests directly:** `./gradlew testDebugUnitTest` 
2. **Integration testing against DAOs/Emulator:** `./gradlew connectedDebugAndroidTest`
3. **Run Code Analyzer/quality tools:** `./gradlew ktlintCheck detekt`
4. **Generating jacoco coverage report:** `./gradlew jacocoTestReport`

If writing scripts for PR-related CI automations, mimic the actions configured inside `.github/workflows/`.
