package com.example.cryptoexchange.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [CryptoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CryptoDatabase : RoomDatabase() {

    abstract fun cryptoDao(): CryptoDao

    companion object {

        @Volatile
        private var INSTANCE: CryptoDatabase? = null

        fun getDatabase(
            context: Context
        ): CryptoDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CryptoDatabase::class.java,
                    "crypto_database"
                )
                    .setDriver(
                        AndroidSQLiteDriver()
                    )
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}