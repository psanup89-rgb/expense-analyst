# Data Models & Database Schema

## Database: Room (SQLite)
- Database class: `ExpenseAnalystDatabase`
- **Current schema version: `12`**
- Room schema export is enabled under `data/schemas/`
- All migrations are inline in `ExpenseAnalystDatabase.kt` (v1→v2→...→v12)
- Home currency preference is stored separately in DataStore, not in Room

---

## Tables

### expenses
Primary table for all transactions (manual and auto-parsed).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | Unique expense ID |
| amount | REAL | NOT NULL | Amount in original currency |
| currency_code | TEXT | NOT NULL | ISO 4217 code (e.g., "INR", "USD", "SAR") |
| home_amount | REAL | NULLABLE | Converted amount in home currency |
| exchange_rate | REAL | NULLABLE | Exchange rate used at time of entry |
| description | TEXT | NOT NULL | Optional user notes (was overloaded pre-v4; now merchant_name is primary) |
| category_id | INTEGER | FK → categories.id | Expense category |
| payment_method | TEXT | NOT NULL | Enum: CASH, UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, OTHER |
| transaction_type | TEXT | NOT NULL | Enum: EXPENSE, INCOME, TRANSFER, PAYMENT |
| date_utc_millis | INTEGER | NOT NULL | Transaction date as UTC epoch milliseconds |
| merchant_name | TEXT | NULLABLE | Merchant/payee name (mandatory in UI for SMS_AUTO/NOTIFICATION_AUTO) |
| source_type | TEXT | NOT NULL | Enum: MANUAL, SMS_AUTO, NOTIFICATION_AUTO |
| source_sender | TEXT | NULLABLE | SMS sender or notification package name |
| account_id | INTEGER | FK → accounts.id, NULLABLE | Linked bank account |
| raw_sms_body | TEXT | NULLABLE | Original SMS text (for SMS_AUTO/NOTIFICATION_AUTO) |
| emi_group_id | INTEGER | FK → emi_groups.id, NULLABLE | Null if standalone expense |
| emi_installment_number | INTEGER | NULLABLE | 1, 2, 3... for EMI entries |
| note | TEXT | NULLABLE | User notes |
| is_deleted | INTEGER | NOT NULL, DEFAULT 0 | Soft delete flag (0=active, 1=deleted) |
| created_at_utc_millis | INTEGER | NOT NULL | Record creation timestamp |
| updated_at_utc_millis | INTEGER | NOT NULL | Last update timestamp |

**Indices:**
- `idx_expenses_date` on `date_utc_millis` (date range queries)
- `idx_expenses_category` on `category_id` (category aggregation)
- `idx_expenses_emi_group` on `emi_group_id` (EMI group lookups)
- `idx_expenses_deleted` on `is_deleted` (filter active records)

### categories
Pre-seeded and user-created expense categories.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | |
| name | TEXT | NOT NULL, UNIQUE | Category name |
| icon_name | TEXT | NOT NULL | Material icon identifier |
| color_hex | TEXT | NOT NULL | Hex color (e.g., "#FF5722") |
| is_default | INTEGER | NOT NULL | 1 if pre-seeded, 0 if user-created |
| sort_order | INTEGER | NOT NULL | Display order |

**Pre-seeded categories:**

| Name | Icon | Color |
|------|------|-------|
| Food | restaurant | #FF5722 |
| Transport | directions_car | #2196F3 |
| Shopping | shopping_bag | #E91E63 |
| Bills | receipt_long | #FF9800 |
| Entertainment | movie | #9C27B0 |
| Health | medical_services | #4CAF50 |
| Education | school | #3F51B5 |
| Groceries | local_grocery_store | #8BC34A |
| Rent | home | #795548 |
| Salary | payments | #CCFF00 |
| Transfer | swap_horiz | #607D8B |
| Other | more_horiz | #9E9E9E |

### emi_groups
Groups of installment payments linked to multiple expense entries.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | |
| total_amount | REAL | NOT NULL | Total EMI amount |
| currency_code | TEXT | NOT NULL | ISO 4217 |
| number_of_installments | INTEGER | NOT NULL | Total months |
| installment_amount | REAL | NOT NULL | Per-month amount |
| interest_rate | REAL | NULLABLE | Optional APR (e.g., 12.0 for 12%) |
| start_date_utc_millis | INTEGER | NOT NULL | First installment date |
| description | TEXT | NOT NULL | EMI description |
| category_id | INTEGER | FK → categories.id | |
| payment_method | TEXT | NOT NULL | |
| created_at_utc_millis | INTEGER | NOT NULL | |

### accounts
Bank accounts linked to expenses (auto-created via `findOrCreate`).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | |
| bank_name | TEXT | NOT NULL | e.g. "Al Rajhi Bank", "Axis Bank" |
| last_four | TEXT | NULLABLE | Last 4 digits of card/account |
| account_type | TEXT | NOT NULL | Enum: SAVINGS, CURRENT, CREDIT_CARD, DEBIT_CARD, FOREX_CARD, WALLET, OTHER |
| display_name | TEXT | NOT NULL | Computed: "Axis Bank *9665 · Forex Card" |

### merchant_rules
User-defined merchant→category mapping for intelligent auto-categorization.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | |
| merchant_pattern | TEXT | NOT NULL, UNIQUE | Pattern matched case-insensitively via `contains` |
| category_id | INTEGER | NOT NULL | FK → categories.id |
| category_name | TEXT | NOT NULL | Denormalized for display without join |
| created_at_utc_millis | INTEGER | NOT NULL | |

**Index**: `idx_merchant_rules_pattern` (UNIQUE) on `merchant_pattern`

### pending_notifications
Transactions detected from SMS/notifications awaiting user action (add or dismiss).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | |
| amount | REAL | NOT NULL | Parsed transaction amount |
| currency_code | TEXT | NOT NULL | ISO 4217 |
| merchant_name | TEXT | NULLABLE | Parsed merchant/payee |
| bank_name | TEXT | NOT NULL | e.g. "Al Rajhi Bank" |
| account_last4 | TEXT | NULLABLE | Last 4 digits |
| transaction_type | TEXT | NOT NULL | DEBIT, CREDIT, or PAYMENT |
| detected_at_millis | INTEGER | NOT NULL | When detected |
| raw_body | TEXT | NULLABLE | Original SMS / notification text (shown as "Source SMS" in AddExpense) |
| payment_method | TEXT | NULLABLE | PaymentMethod enum name e.g. "APPLE_PAY" |
| is_possible_duplicate | INTEGER | NOT NULL, DEFAULT 0 | Flag set when a similar pending notification already exists (added in v11→v12) |

### bills
Credit card statements, utility bills, and subscriptions parsed from notification/SMS. Added in DB migration v9→v10.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | |
| biller_name | TEXT | NOT NULL | e.g. "Axis Bank", "ENBD" |
| account_id | INTEGER | FK → accounts.id, NULLABLE | Linked account |
| total_due | REAL | NULLABLE | Total amount due |
| minimum_due | REAL | NULLABLE | Minimum payment amount |
| currency_code | TEXT | NOT NULL | ISO 4217 |
| due_date_millis | INTEGER | NULLABLE | Bill due date (UTC epoch ms) |
| statement_period_start_millis | INTEGER | NULLABLE | Statement period start (UTC epoch ms) |
| statement_period_end_millis | INTEGER | NULLABLE | Statement period end (UTC epoch ms) |
| status | TEXT | NOT NULL | Enum: OPEN, PAID, OVERDUE |
| source_type | TEXT | NOT NULL | How bill was detected (e.g. NOTIFICATION_AUTO) |
| created_at_millis | INTEGER | NOT NULL | Record creation timestamp |
| is_deleted | INTEGER | NOT NULL, DEFAULT 0 | Soft delete flag |

### tags
User-defined tags for labelling expenses. Added in DB migration v10→v11.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INTEGER | PK, autoGenerate | |
| name | TEXT | NOT NULL, UNIQUE | Tag label (e.g. "work", "travel") |

**Index**: unique on `name`

### expense_tags
Junction table linking expenses to tags (many-to-many). Added in DB migration v10→v11.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| expense_id | INTEGER | FK → expenses.id, CASCADE | |
| tag_id | INTEGER | FK → tags.id, CASCADE | |

**Index**: on `tag_id`

### currency_rates
Cached exchange rates for offline support.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| currency_code | TEXT | PK | ISO 4217 code |
| rate_to_base | REAL | NOT NULL | Rate relative to USD (base) |
| last_updated_utc_millis | INTEGER | NOT NULL | When rate was fetched |

### preferences (DataStore, not Room)
- File: `expense_analyst_preferences.preferences_pb`
- Current key: `home_currency_code`
- Current fallback default in code: `SAR`

---

## Domain Models (Kotlin)

Domain models live in `:domain/model/` and are distinct from Room entities.

```kotlin
data class Expense(
    val id: Long = 0,
    val amount: Double,
    val currencyCode: String,
    val homeAmount: Double?,
    val exchangeRate: Double?,
    val description: String,        // optional user notes (was overloaded pre-v4)
    val category: Category,
    val paymentMethod: PaymentMethod,
    val transactionType: TransactionType,
    val date: Instant,              // kotlinx-datetime
    val merchantName: String?,      // primary identifier; mandatory in UI for auto-imported
    val sourceType: SourceType,
    val sourceSender: String? = null,
    val accountId: Long? = null,    // FK to Account
    val rawSmsBody: String? = null, // original SMS text
    val emiGroupId: Long? = null,
    val emiInstallmentNumber: Int? = null,
    val note: String?,
    val isDeleted: Boolean = false
)

data class Account(
    val id: Long = 0,
    val bankName: String,
    val lastFour: String?,
    val accountType: AccountType,
    val displayName: String         // computed: "Axis Bank *9665 · Forex Card"
)

data class MerchantRule(
    val id: Long = 0,
    val merchantPattern: String,    // matched case-insensitively via contains
    val categoryId: Long,
    val categoryName: String,       // denormalized
    val createdAt: Long
)

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean,
    val sortOrder: Int
)

data class EmiGroup(
    val id: Long = 0,
    val totalAmount: Double,
    val currencyCode: String,
    val numberOfInstallments: Int,
    val installmentAmount: Double,
    val interestRate: Double?,
    val startDate: Instant,
    val description: String,
    val category: Category,
    val paymentMethod: PaymentMethod,
    val paidCount: Int = 0          // computed from linked expenses
)

data class CurrencyRate(
    val currencyCode: String,
    val rateToBase: Double,
    val lastUpdated: Instant
)
```

## Enums

```kotlin
enum class PaymentMethod {
    CASH, UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, OTHER
}

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER, PAYMENT   // PAYMENT = bill/card payment (purple, excluded from totals)
}

enum class AccountType(val label: String) {
    SAVINGS("Savings"), CURRENT("Current"),
    CREDIT_CARD("Credit Card"), DEBIT_CARD("Debit Card"), FOREX_CARD("Forex Card"),
    WALLET("Wallet"), OTHER("Account")
}

enum class SourceType {
    MANUAL, SMS_AUTO, NOTIFICATION_AUTO
}

// Parser layer only (feature/notification):
enum class TransactionDirection { DEBIT, CREDIT, PAYMENT }
```

## Entity ↔ Domain Mapping
Mappers in `data/mapper/` are Kotlin extension functions, not mapper classes:
- `fun ExpenseWithCategory.toDomain(): Expense` — uses Room `@Relation` to join category inline
- `fun Expense.toEntity(createdAt: Long, updatedAt: Long): ExpenseEntity`
- `fun CategoryEntity.toDomain(): Category` / `fun Category.toEntity(): CategoryEntity`
- Date conversion: `Instant.toEpochMilliseconds()` ↔ `Instant.fromEpochMilliseconds()`

`createdAtUtcMillis` is NOT on the domain `Expense` model — it is a persistence concern. On `updateExpense`, the repository fetches the existing entity first to preserve its `createdAtUtcMillis` before writing.

## Snapshot / Repair Support
The repository layer now supports expense snapshots for repair and backfill work:
- `ExpenseRepository.getExpensesSnapshot(includeDeleted: Boolean)`

This is currently used by the home-currency repair flow so stale `homeAmount` / `exchangeRate` values can be rewritten after a currency change.

## EMI Installment Calculation

```
Without interest:
  installmentAmount = totalAmount / numberOfInstallments

With interest (EMI formula):
  monthlyRate = annualRate / 12 / 100
  installmentAmount = totalAmount * monthlyRate * (1 + monthlyRate)^n / ((1 + monthlyRate)^n - 1)
  where n = numberOfInstallments
```

Each installment creates an `ExpenseEntity` with:
- `emiGroupId` = the group ID
- `emiInstallmentNumber` = 1, 2, 3... n
- `dateUtcMillis` = start date + (installmentNumber - 1) months
- `amount` = calculated installment amount

## Migration Strategy
- Use Room's `@Database(version = N)` and `Migration(N, N+1)` classes
- Add a dedicated migration package when the schema moves past version 1
- Test migrations using Room's `MigrationTestHelper`
- Never lose user data — always migrate, never recreate
