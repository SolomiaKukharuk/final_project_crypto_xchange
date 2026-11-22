package com.example.cryptoexchange.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CryptoDao {

    @Query(
        "SELECT * FROM cryptocurrencies " +
                "ORDER BY rank ASC"
    )
    abstract fun observeCryptocurrencies():
            Flow<List<CryptoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun upsertAll(
        currencies: List<CryptoEntity>
    ): List<Long>

    @Query("DELETE FROM cryptocurrencies")
    abstract fun deleteAll(): Int

    @Transaction
    open fun replaceAll(
        currencies: List<CryptoEntity>
    ) {
        deleteAll()
        upsertAll(currencies)
    }
}