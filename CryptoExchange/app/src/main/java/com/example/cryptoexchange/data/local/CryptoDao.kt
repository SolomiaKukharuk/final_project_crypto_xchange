package com.example.cryptoexchange.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoDao {

    @Query(
        "SELECT * FROM cryptocurrencies " +
                "ORDER BY rank ASC"
    )
    fun observeCryptocurrencies():
            Flow<List<CryptoEntity>>

    @Upsert
    suspend fun upsertAll(
        currencies: List<CryptoEntity>
    )

    @Query("DELETE FROM cryptocurrencies")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(
        currencies: List<CryptoEntity>
    ) {
        deleteAll()
        upsertAll(currencies)
    }
}