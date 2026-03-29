---
## skill: transaction-direction-enum-extension
agent: ParserAgent, FeatureAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [parser, enum, transactiondirection, checklist]

# Adding a New TransactionDirection Value

## When to use this

When a new `TransactionDirection` value needs to be added to `ParsedTransaction.kt` (e.g. `TRANSFER`, a future `REFUND` direction, etc.), there are several downstream files that must be updated to handle the new value. Forgetting any one of them causes either a compilation error (exhaustive `when`) or silently incorrect behaviour.

## What to do

1. **`ParsedTransaction.kt`** — Add the new value to the enum:
   ```kotlin
   enum class TransactionDirection { DEBIT, CREDIT, PAYMENT, TRANSFER }
   ```

2. **`SmsImportViewModel.kt`** — Add a branch to the `when` block that maps direction → `TransactionType`:
   ```kotlin
   val transactionType = when (parsed.type) {
       TransactionDirection.CREDIT   -> TransactionType.INCOME
       TransactionDirection.DEBIT    -> TransactionType.EXPENSE
       TransactionDirection.PAYMENT  -> TransactionType.PAYMENT
       TransactionDirection.TRANSFER -> TransactionType.TRANSFER  // ← add
   }
   ```
   This block is exhaustive — Kotlin will flag a compile error if you miss a branch.

3. **`TransactionAlertNotification.kt`** — Update the direction label used in the system tray notification title:
   ```kotlin
   val direction = when (parsed.type) {
       TransactionDirection.DEBIT    -> "Spent"
       TransactionDirection.TRANSFER -> "Transfer"  // ← add
       else -> "Received"
   }
   ```

4. **`NotificationBanner.kt`** — Update the label used in the in-app banner text:
   ```kotlin
   val direction = when (tx.type) {
       TransactionDirection.DEBIT    -> "spent"
       TransactionDirection.TRANSFER -> "transferred"  // ← add
       else -> "received"
   }
   ```

5. **`SmsImportScreen.kt`** — Uses an `if/else` for icon tint (debit = red, else = green). New directions fall into the `else` branch (treated like CREDIT visually). Only update if the new direction needs a distinct visual treatment.

6. **`AddExpenseViewModel.kt`** — The nav-arg `type` string mapping already handles `"TRANSFER"` (line ~64). Verify the new direction's `.name` matches what this `when` block expects.

## Example

Adding `TRANSFER` for Al Rajhi internal bank transfer SMS:
- `ParsedTransaction.kt`: `enum class TransactionDirection { DEBIT, CREDIT, PAYMENT, TRANSFER }`
- `SmsImportViewModel.kt`: `TransactionDirection.TRANSFER -> TransactionType.TRANSFER`
- `TransactionAlertNotification.kt`: `TransactionDirection.TRANSFER -> "Transfer"`
- `NotificationBanner.kt`: `TransactionDirection.TRANSFER -> "transferred"`

## Pitfalls

- `SmsImportViewModel`'s `when` is exhaustive — you get a compile error if you miss it. The other files use `if/else` or `else` branches — **they compile silently even if the new direction falls through incorrectly.**
- `AddExpenseViewModel` maps the type as a nav-arg String, not as the enum directly. The `.name` property of the enum value must match the string case in that `when` block.

## Related skills

- `parser-body-fingerprint.md` — for writing the parser `canParse()` that produces this direction
- `new-domain-entity.md` — if the direction change requires a new domain model field
