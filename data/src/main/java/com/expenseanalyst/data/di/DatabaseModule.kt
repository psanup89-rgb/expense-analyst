package com.expenseanalyst.data.di

import android.content.Context
import com.expenseanalyst.data.local.ExpenseAnalystDatabase
import com.expenseanalyst.data.local.dao.AccountDao
import com.expenseanalyst.data.local.dao.CategoryDao
import com.expenseanalyst.data.local.dao.CurrencyRateDao
import com.expenseanalyst.data.local.dao.EmiGroupDao
import com.expenseanalyst.data.local.dao.ExpenseDao
import com.expenseanalyst.data.local.dao.MerchantRuleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseAnalystDatabase {
        return ExpenseAnalystDatabase.buildDatabase(context)
    }

    @Provides
    fun provideExpenseDao(database: ExpenseAnalystDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideCategoryDao(database: ExpenseAnalystDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideEmiGroupDao(database: ExpenseAnalystDatabase): EmiGroupDao {
        return database.emiGroupDao()
    }

    @Provides
    fun provideCurrencyRateDao(database: ExpenseAnalystDatabase): CurrencyRateDao {
        return database.currencyRateDao()
    }

    @Provides
    fun provideAccountDao(database: ExpenseAnalystDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideMerchantRuleDao(database: ExpenseAnalystDatabase): MerchantRuleDao {
        return database.merchantRuleDao()
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
