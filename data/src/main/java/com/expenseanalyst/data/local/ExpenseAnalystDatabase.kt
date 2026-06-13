package com.expenseanalyst.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expenseanalyst.data.local.dao.AccountDao
import com.expenseanalyst.data.local.dao.CategoryDao
import com.expenseanalyst.data.local.dao.CurrencyRateDao
import com.expenseanalyst.data.local.dao.EmiGroupDao
import com.expenseanalyst.data.local.dao.ExpenseDao
import com.expenseanalyst.data.local.dao.BillDao
import com.expenseanalyst.data.local.dao.MerchantRuleDao
import com.expenseanalyst.data.local.dao.PendingNotificationDao
import com.expenseanalyst.data.local.dao.LentItemDao
import com.expenseanalyst.data.local.dao.PlannedExpenseDao
import com.expenseanalyst.data.local.dao.SalaryDao
import com.expenseanalyst.data.local.dao.TagDao
import com.expenseanalyst.data.local.entity.AccountEntity
import com.expenseanalyst.data.local.entity.BillEntity
import com.expenseanalyst.data.local.entity.CategoryEntity
import com.expenseanalyst.data.local.entity.CurrencyRateEntity
import com.expenseanalyst.data.local.entity.EmiGroupEntity
import com.expenseanalyst.data.local.entity.ExpenseEntity
import com.expenseanalyst.data.local.entity.ExpenseTagCrossRef
import com.expenseanalyst.data.local.entity.LentItemEntity
import com.expenseanalyst.data.local.entity.MerchantRuleEntity
import com.expenseanalyst.data.local.entity.PendingNotificationEntity
import com.expenseanalyst.data.local.entity.PlannedExpenseEntity
import com.expenseanalyst.data.local.entity.SalaryEntryEntity
import com.expenseanalyst.data.local.entity.TagEntity


@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        EmiGroupEntity::class,
        CurrencyRateEntity::class,
        AccountEntity::class,
        MerchantRuleEntity::class,
        PendingNotificationEntity::class,
        BillEntity::class,
        TagEntity::class,
        ExpenseTagCrossRef::class,
        SalaryEntryEntity::class,
        PlannedExpenseEntity::class,
        LentItemEntity::class
    ],
    version = 19,
    exportSchema = true
)
abstract class ExpenseAnalystDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun emiGroupDao(): EmiGroupDao
    abstract fun currencyRateDao(): CurrencyRateDao
    abstract fun accountDao(): AccountDao
    abstract fun merchantRuleDao(): MerchantRuleDao
    abstract fun pendingNotificationDao(): PendingNotificationDao
    abstract fun billDao(): BillDao
    abstract fun tagDao(): TagDao
    abstract fun salaryDao(): SalaryDao
    abstract fun plannedExpenseDao(): PlannedExpenseDao
    abstract fun lentItemDao(): LentItemDao

    companion object {
        const val DATABASE_NAME = "expense_analyst.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (name, icon_name, color_hex, is_default, sort_order) " +
                        "VALUES ('Misc', 'help_outline', '#BDBDBD', 1, 12)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN account_number TEXT")
                db.execSQL("UPDATE expenses SET transaction_type = 'EXPENSE' WHERE transaction_type = 'DEBIT'")
                db.execSQL("UPDATE expenses SET transaction_type = 'INCOME' WHERE transaction_type = 'CREDIT'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN raw_sms_body TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN payment_method TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        biller_name TEXT NOT NULL,
                        account_id INTEGER,
                        total_due REAL,
                        minimum_due REAL,
                        currency_code TEXT NOT NULL,
                        due_date_millis INTEGER,
                        statement_period_start_millis INTEGER,
                        statement_period_end_millis INTEGER,
                        status TEXT NOT NULL,
                        source_type TEXT NOT NULL,
                        created_at_millis INTEGER NOT NULL,
                        is_deleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE expenses ADD COLUMN bill_id INTEGER")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create tags table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_tags_name ON tags (name)")

                // Create junction table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expense_tags (
                        expense_id INTEGER NOT NULL,
                        tag_id INTEGER NOT NULL,
                        PRIMARY KEY (expense_id, tag_id),
                        FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
                        FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_expense_tags_tag ON expense_tags (tag_id)")

                // Pre-seed default tags
                val defaultTags = listOf(
                    "Recurring", "One-time", "Reimbursable", "Tax Deductible",
                    "Personal", "Business", "Shared", "Subscription", "Essential"
                )
                defaultTags.forEach { tag ->
                    db.execSQL("INSERT OR IGNORE INTO tags (name) VALUES ('$tag')")
                }

            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pending_notifications ADD COLUMN is_possible_duplicate INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN reference TEXT")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN pending_type TEXT NOT NULL DEFAULT 'TRANSACTION'")
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN biller_name TEXT")
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN due_date_millis INTEGER")
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN linked_bill_id INTEGER")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS salary_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        currency_code TEXT NOT NULL,
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        source_expense_id INTEGER,
                        is_confirmed INTEGER NOT NULL DEFAULT 1,
                        created_at_millis INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_salary_entries_month_year ON salary_entries (month, year)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS planned_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        currency_code TEXT NOT NULL,
                        category_id INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        is_deleted INTEGER NOT NULL DEFAULT 0,
                        created_at_millis INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT OR IGNORE INTO categories (name, icon_name, color_hex, is_default, sort_order) " +
                        "VALUES ('Refund', 'currency_exchange', '#26C6DA', 1, 13)"
                )
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN needs_review INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS lent_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        person_name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        currency_code TEXT NOT NULL,
                        home_amount REAL,
                        description TEXT NOT NULL,
                        lent_date_millis INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        settled_amount REAL,
                        settled_date_millis INTEGER,
                        linked_expense_id INTEGER,
                        settlement_expense_id INTEGER,
                        reminder_datetime_millis INTEGER,
                        is_deleted INTEGER NOT NULL DEFAULT 0,
                        created_at_millis INTEGER NOT NULL,
                        updated_at_millis INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lent_items_status ON lent_items (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lent_items_is_deleted ON lent_items (is_deleted)")
            }
        }

        // MIGRATION_14_15 created the salary_entries unique index with the wrong name
        // (idx_salary_month_year instead of the Room-generated index_salary_entries_month_year).
        // This migration drops the misnamed index and creates the correct one.
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS idx_salary_month_year")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_salary_entries_month_year ON salary_entries (month, year)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_notifications ADD COLUMN raw_body TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        currency_code TEXT NOT NULL,
                        merchant_name TEXT,
                        bank_name TEXT NOT NULL,
                        account_last4 TEXT,
                        transaction_type TEXT NOT NULL,
                        detected_at_millis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchant_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        merchant_pattern TEXT NOT NULL,
                        category_id INTEGER NOT NULL,
                        category_name TEXT NOT NULL,
                        created_at_utc_millis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_merchant_rules_pattern ON merchant_rules (merchant_pattern)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bank_name TEXT NOT NULL,
                        last_four TEXT,
                        account_type TEXT NOT NULL,
                        display_name TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_bank_last4 ON accounts (bank_name, last_four)"
                )
                db.execSQL("ALTER TABLE expenses ADD COLUMN account_id INTEGER REFERENCES accounts(id) ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_account ON expenses (account_id)")
            }
        }

        fun buildDatabase(context: Context): ExpenseAnalystDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ExpenseAnalystDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                .addCallback(SeedDatabaseCallback())
                .build()
        }
    }

    private class SeedDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val defaultCategories = listOf(
                "('Food', 'restaurant', '#FF5722', 1, 0)",
                "('Transport', 'directions_car', '#2196F3', 1, 1)",
                "('Shopping', 'shopping_bag', '#E91E63', 1, 2)",
                "('Bills', 'receipt_long', '#FF9800', 1, 3)",
                "('Entertainment', 'movie', '#9C27B0', 1, 4)",
                "('Health', 'medical_services', '#4CAF50', 1, 5)",
                "('Education', 'school', '#3F51B5', 1, 6)",
                "('Groceries', 'local_grocery_store', '#8BC34A', 1, 7)",
                "('Rent', 'home', '#795548', 1, 8)",
                "('Salary', 'payments', '#CCFF00', 1, 9)",
                "('Transfer', 'swap_horiz', '#607D8B', 1, 10)",
                "('Other', 'more_horiz', '#9E9E9E', 1, 11)",
                "('Misc', 'help_outline', '#BDBDBD', 1, 12)",
                "('Refund', 'currency_exchange', '#26C6DA', 1, 13)"
            )
            defaultCategories.forEach { values ->
                db.execSQL("INSERT INTO categories (name, icon_name, color_hex, is_default, sort_order) VALUES $values")
            }
            // Seed default tags for fresh installs
            val defaultTags = listOf(
                "Recurring", "One-time", "Reimbursable", "Tax Deductible",
                "Personal", "Business", "Shared", "Subscription", "Essential"
            )
            defaultTags.forEach { tag ->
                db.execSQL("INSERT OR IGNORE INTO tags (name) VALUES ('$tag')")
            }
        }
    }
}
