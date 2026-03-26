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
import com.expenseanalyst.data.local.dao.MerchantRuleDao
import com.expenseanalyst.data.local.entity.AccountEntity
import com.expenseanalyst.data.local.entity.CategoryEntity
import com.expenseanalyst.data.local.entity.CurrencyRateEntity
import com.expenseanalyst.data.local.entity.EmiGroupEntity
import com.expenseanalyst.data.local.entity.ExpenseEntity
import com.expenseanalyst.data.local.entity.MerchantRuleEntity


@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        EmiGroupEntity::class,
        CurrencyRateEntity::class,
        AccountEntity::class,
        MerchantRuleEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class ExpenseAnalystDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun emiGroupDao(): EmiGroupDao
    abstract fun currencyRateDao(): CurrencyRateDao
    abstract fun accountDao(): AccountDao
    abstract fun merchantRuleDao(): MerchantRuleDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
                "('Misc', 'help_outline', '#BDBDBD', 1, 12)"
            )
            defaultCategories.forEach { values ->
                db.execSQL("INSERT INTO categories (name, icon_name, color_hex, is_default, sort_order) VALUES $values")
            }
        }
    }
}
