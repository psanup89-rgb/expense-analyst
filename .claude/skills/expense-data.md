# Expense Data Skill

You are a specialist in maintaining the Data Models, Room Database Schemas, and the Domain translation mapping within the **Expense Analyst** Android app.

## Project Structure Overview
Expense Analyst strictly separates its database representation (`@Entity`) from its business representation (`Domain Models`).
The entities live in `:data/local/entity/` and models reside in `:domain/model/`.

### The Rule of Conversion
Mappers in `data/mapper/` are **Kotlin extension functions**, not mapper classes:
1. `fun Expense.toEntity(createdAt: Long, updatedAt: Long): ExpenseEntity`
2. `fun ExpenseWithCategory.toDomain(): Expense` — uses Room `@Embedded` + `@Relation` to join the category; the DAO returns `ExpenseWithCategory` objects directly
3. `fun CategoryEntity.toDomain(): Category` / `fun Category.toEntity(): CategoryEntity`

`createdAtUtcMillis` is a persistence concern NOT in the domain `Expense` model. On `updateExpense`, fetch the existing entity first to preserve `createdAtUtcMillis`.

## Room Schema Definition
Database: `ExpenseAnalystDatabase`

### Table `expenses`
Contains every transaction entry (both manual and auto-parsed).

Key Columns:
- `amount` & `currency_code` (original value)
- `home_amount` & `exchange_rate` (converted equivalent offline cache, if differing)
- `category_id` (FK to `categories`)
- `payment_method` (Enum value matching CASH, UPI, CREDIT_CARD, etc.)
- `date_utc_millis` (Timestamps stored exactly as UTC Epoch Milliseconds)
- `source_type` (Enum: MANUAL, SMS_AUTO, NOTIFICATION_AUTO)
- `is_deleted` (Soft delete flag. 0 = active, 1 = deleted). Never hard-delete records.

### Table `categories`
Contains default configured and user-added categories.
Key Columns:
- `name` (unique identifier)
- `icon_name` (reference to material icon)
- `color_hex` (colors representing categories)
- `is_default` (flag identifying pre-seeded items that cannot be permanently deleted)

### Table `emi_groups`
Holds installment plans that link multiple `expenses` together via `emi_group_id`.
Key Columns:
- `total_amount` & `installment_amount`
- `number_of_installments`
- `start_date_utc_millis` (Epoch in UTC)

### Table `currency_rates`
Caches fetched ExchangeRate-API data.
- `currency_code` (ISO 4217, e.g., INR)
- `rate_to_base` (Conversion factor against base USD)
- `last_updated_utc_millis` (Fetch timestamp)

## EMI Logic & Computations
When creating EMI from an `Expense` object, use the exact formula for EMI generation over the requested `numberOfInstallments`:
`installmentAmount = totalAmount * (rate/12/100) * (1 + rate/12/100)^n / ((1 + rate/12/100)^n - 1)`
Alternatively, without interest: `installmentAmount = totalAmount / numberOfInstallments`.

Link every future date-computed expense to the created `EmiGroup` utilizing `emiGroupId` and indexing via `emiInstallmentNumber`.

## Database Rules
1. Never perform destructive DML (such as DROP statements directly on tables).
2. All new variations MUST include a proper Room `Migration(N, N+1)` inside `data/local/migration/Migrations.kt`.
3. Never use hard deletes for user generated data (`expenses`, `emi_groups`). Enable Soft Delete via `is_deleted` toggle.
