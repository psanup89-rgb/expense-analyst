package com.expenseanalyst.data.di

import com.expenseanalyst.data.repository.AccountRepositoryImpl
import com.expenseanalyst.data.repository.BillRepositoryImpl
import com.expenseanalyst.data.repository.MerchantRuleRepositoryImpl
import com.expenseanalyst.data.repository.MerchantSearchRepositoryImpl
import com.expenseanalyst.data.repository.PendingNotificationRepositoryImpl
import com.expenseanalyst.data.repository.AppPreferencesRepositoryImpl
import com.expenseanalyst.data.repository.CategoryRepositoryImpl
import com.expenseanalyst.data.repository.CurrencyRepositoryImpl
import com.expenseanalyst.data.repository.EmiRepositoryImpl
import com.expenseanalyst.data.repository.ExpenseRepositoryImpl
import com.expenseanalyst.data.repository.OnboardingRepositoryImpl
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.repository.MerchantSearchRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.EmiRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindEmiRepository(impl: EmiRepositoryImpl): EmiRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(impl: AppPreferencesRepositoryImpl): AppPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindMerchantRuleRepository(impl: MerchantRuleRepositoryImpl): MerchantRuleRepository

    @Binds
    @Singleton
    abstract fun bindPendingNotificationRepository(impl: PendingNotificationRepositoryImpl): PendingNotificationRepository

    @Binds
    @Singleton
    abstract fun bindBillRepository(impl: BillRepositoryImpl): BillRepository

    @Binds
    @Singleton
    abstract fun bindMerchantSearchRepository(impl: MerchantSearchRepositoryImpl): MerchantSearchRepository
}
