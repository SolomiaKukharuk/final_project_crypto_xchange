package com.example.cryptoexchange.di

import android.content.Context
import com.example.cryptoexchange.BuildConfig
import com.example.cryptoexchange.data.local.CryptoDatabase
import com.example.cryptoexchange.data.remote.CryptoNetworkModule
import com.example.cryptoexchange.data.repository.CryptoRepositoryImpl
import com.example.cryptoexchange.data.worker.CryptoSyncScheduler
import com.example.cryptoexchange.domain.repository.CryptoRepository
import com.example.cryptoexchange.domain.usecase.SyncCryptocurrenciesUseCase
import com.example.cryptoexchange.domain.usecase.ObserveCryptocurrenciesUseCase

class AppContainer(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val database =
        CryptoDatabase.getDatabase(
            appContext
        )

    private val cryptoDao =
        database.cryptoDao()

    private val cryptoApi =
        CryptoNetworkModule.api

    val cryptoRepository: CryptoRepository =
        CryptoRepositoryImpl(
            api = cryptoApi,
            dao = cryptoDao,
            apiKey = BuildConfig.COINDESK_API_KEY
        )

    val syncCryptocurrenciesUseCase =
        SyncCryptocurrenciesUseCase(
            repository = cryptoRepository
        )

    val observeCryptocurrenciesUseCase =
        ObserveCryptocurrenciesUseCase(
            repository = cryptoRepository
        )

    val cryptoSyncScheduler =
        CryptoSyncScheduler(
            context = appContext
        )
}