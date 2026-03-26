package com.expenseanalyst.data.repository

import com.expenseanalyst.data.local.dao.AccountDao
import com.expenseanalyst.data.local.entity.AccountEntity
import com.expenseanalyst.data.mapper.toDomain
import com.expenseanalyst.data.mapper.toEntity
import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }

    override fun getAccountById(id: Long): Flow<Account?> =
        accountDao.getAccountById(id).map { it?.toDomain() }

    override suspend fun addAccount(account: Account): Long =
        accountDao.insertAccount(account.toEntity())

    override suspend fun updateAccount(account: Account) =
        accountDao.updateAccount(account.toEntity())

    override suspend fun findOrCreate(
        bankName: String,
        lastFour: String?,
        accountType: AccountType
    ): Long {
        accountDao.findByBankAndLastFour(bankName, lastFour)?.let { return it.id }
        val displayName = buildDisplayName(bankName, lastFour, accountType)
        val entity = AccountEntity(
            bankName = bankName,
            lastFour = lastFour,
            accountType = accountType.name,
            displayName = displayName
        )
        val inserted = accountDao.insertAccount(entity)
        if (inserted != -1L) return inserted
        return accountDao.findByBankAndLastFour(bankName, lastFour)!!.id
    }

    private fun buildDisplayName(bankName: String, lastFour: String?, type: AccountType): String =
        if (lastFour != null) "$bankName *$lastFour · ${type.label}"
        else "$bankName · ${type.label}"
}
