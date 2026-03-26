package com.expenseanalyst.domain.repository

import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getAccountById(id: Long): Flow<Account?>
    suspend fun addAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun findOrCreate(bankName: String, lastFour: String?, accountType: AccountType): Long
}
