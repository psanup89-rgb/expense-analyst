package com.expenseanalyst.data.local.dao

import androidx.room.*
import com.expenseanalyst.data.local.entity.CurrencyRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyRateDao {

    @Query("SELECT * FROM currency_rates ORDER BY currency_code ASC")
    fun getAllRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE currency_code = :code")
    fun getRate(code: String): Flow<CurrencyRateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<CurrencyRateEntity>)

    @Query("SELECT MAX(last_updated_utc_millis) FROM currency_rates")
    suspend fun getLastUpdated(): Long?
}
