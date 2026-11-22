package com.example.cryptoexchange.data.local

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "cryptocurrencies")
data class CryptoEntity(

    @PrimaryKey
    val id: String,

    val name: String,
    val symbol: String,
    val logoUrl: String?,
    val priceUsd: Double?,
    val rank: Int,
    val updatedAt: Long
)