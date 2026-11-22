package com.example.cryptoexchange.data.remote

import com.google.gson.annotations.SerializedName

data class CryptoTopListResponse(
    @SerializedName("Data")
    val data: CryptoTopListData?
)

data class CryptoTopListData(
    @SerializedName("LIST")
    val list: List<CryptoAssetDto>?
)

data class CryptoAssetDto(
    @SerializedName("ID")
    val id: Long?,

    @SerializedName("NAME")
    val name: String?,

    @SerializedName("SYMBOL")
    val symbol: String?,

    @SerializedName("LOGO_URL")
    val logoUrl: String?,

    @SerializedName("PRICE_USD")
    val priceUsd: Double?
)