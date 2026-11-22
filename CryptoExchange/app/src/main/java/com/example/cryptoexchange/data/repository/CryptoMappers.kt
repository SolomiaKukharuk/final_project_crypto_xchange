package com.example.cryptoexchange.data.repository

import com.example.cryptoexchange.data.local.CryptoEntity
import com.example.cryptoexchange.data.remote.CryptoAssetDto
import com.example.cryptoexchange.domain.model.CryptoCurrency

fun CryptoAssetDto.toEntity(
    rank: Int,
    updatedAt: Long
): CryptoEntity? {

    val validId = id ?: return null

    val validName =
        name
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

    val validSymbol =
        symbol
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

    val validLogoUrl =
        logoUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    val validPrice =
        priceUsd
            ?.takeIf {
                it.isFinite() && it >= 0.0
            }

    return CryptoEntity(
        id = validId.toString(),
        name = validName,
        symbol = validSymbol,
        logoUrl = validLogoUrl,
        priceUsd = validPrice,
        rank = rank,
        updatedAt = updatedAt
    )
}

fun CryptoEntity.toDomain(): CryptoCurrency {

    return CryptoCurrency(
        id = id,
        name = name,
        symbol = symbol,
        logoUrl = logoUrl,
        priceUsd = priceUsd,
        rank = rank,
        updatedAt = updatedAt
    )
}