package com.example.cryptoexchange.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CryptoApi {

    @GET("asset/v1/top/list")
    suspend fun getTopCryptocurrencies(
        @Header("Authorization")
        authorization: String,

        @Query("page")
        page: Int = 1,

        @Query("page_size")
        pageSize: Int = 30,

        @Query("sort_by")
        sortBy: String = "CIRCULATING_MKT_CAP_USD",

        @Query("sort_direction")
        sortDirection: String = "DESC",

        @Query("toplist_quote_asset")
        quoteAsset: String = "USD",

        @Query("groups")
        groups: String = "ID,BASIC,PRICE"
    ): Response<CryptoTopListResponse>
}