# Room Migration Gotchas

## Index Name Mismatch — the #1 migration crash

### The problem
When you write a `CREATE INDEX` statement in a Room migration, the index name you choose must **exactly match** what Room's validation expects. Room auto-generates index names using the convention:

```
index_<tableName>_<col1>_<col2>
```

If the name in your SQL doesn't match, Room throws on every app launch:
```
java.lang.IllegalStateException: Migration didn't properly handle: <tableName>
  Expected: index_<tableName>_<col1>_<col2>
  Found: <your custom name>
```

### Real example from this project
`MIGRATION_14_15` originally wrote:
```sql
-- WRONG — custom name, does not match Room's expectation
CREATE UNIQUE INDEX IF NOT EXISTS idx_salary_month_year ON salary_entries (month, year)
```

But `SalaryEntryEntity` uses:
```kotlin
@Entity(
    tableName = "salary_entries",
    indices = [Index(value = ["month", "year"], unique = true)]  // no name= specified
)
```

Room auto-generates the name `index_salary_entries_month_year`. Mismatch → crash.

### Two ways to avoid this

**Option A — Use Room's auto-generated name in the migration SQL (recommended):**
```sql
CREATE UNIQUE INDEX IF NOT EXISTS index_salary_entries_month_year ON salary_entries (month, year)
```

**Option B — Specify the custom name in the entity annotation:**
```kotlin
@Entity(
    tableName = "salary_entries",
    indices = [Index(name = "idx_salary_month_year", value = ["month", "year"], unique = true)]
)
```
Then the migration SQL with `idx_salary_month_year` matches.

### Fix when the bad migration has already shipped

Add a new migration (bump DB version) that renames the index:
```kotlin
private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop the misnamed index (IF EXISTS = no-op on fresh installs)
        db.execSQL("DROP INDEX IF EXISTS idx_salary_month_year")
        // Create with the correct name (IF NOT EXISTS = no-op on fresh installs)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_salary_entries_month_year ON salary_entries (month, year)")
    }
}
```

The `IF EXISTS` / `IF NOT EXISTS` guards make this idempotent: devices that ran the bad migration get fixed, fresh installs are unaffected.

### Checklist before merging any migration

- [ ] Every `CREATE INDEX` name matches `index_<tableName>_<col1>_<col2>`
- [ ] OR the entity's `@Index` annotation has a matching explicit `name = "..."` 
- [ ] Run the app on a device that previously had the old DB to verify no crash
- [ ] Run the app on a fresh install to verify no crash

### How to diagnose a migration crash from logcat

```bash
adb logcat -c
adb shell monkey -p com.expenseanalyst 1
sleep 6
adb logcat -d | grep -A 80 "FATAL EXCEPTION"
```

Look for `IllegalStateException: Migration didn't properly handle` — the error body shows both **Expected** and **Found** table/index schemas side by side.
