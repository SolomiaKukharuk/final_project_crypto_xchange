package com.example.cryptoexchange.data.repository

import com.example.cryptoexchange.data.local.CryptoDao
import com.example.cryptoexchange.data.remote.CryptoApi
import com.example.cryptoexchange.domain.model.CryptoCurrency
import com.example.cryptoexchange.domain.repository.CryptoRepository
import com.example.cryptoexchange.domain.repository.CryptoSyncException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class CryptoRepositoryImpl(
    private val api: CryptoApi,
    private val dao: CryptoDao,
    private val apiKey: String
) : CryptoRepository {

    override fun observeCryptocurrencies():
            Flow<List<CryptoCurrency>> {

        return dao
            .observeCryptocurrencies()
            .map { entities ->
                entities.map { entity ->
                    entity.toDomain()
                }
            }
    }

    override suspend fun syncCryptocurrencies() {

        if (apiKey.isBlank()) {
            throw CryptoSyncException.Authentication()
        }

        val response =
            try {
                api.getTopCryptocurrencies(
                    authorization = "Apikey $apiKey"
                )
            } catch (e: IOException) {

                throw CryptoSyncException.Temporary(
                    "Немає з'єднання з мережею"
                )
            }

        when (response.code()) {

            401 -> {
                throw CryptoSyncException.Authentication(
                    "401: API key відсутній або невалідний"
                )
            }

            403 -> {
                throw CryptoSyncException.Authentication(
                    "403: API key прийнятий, але немає доступу до цього endpoint"
                )
            }

            429 -> {
                throw CryptoSyncException.RateLimit()
            }
        }

        if (response.code() in 500..599) {
            throw CryptoSyncException.Temporary(
                "Тимчасова помилка сервера: ${response.code()}"
            )
        }

        if (!response.isSuccessful) {
            throw CryptoSyncException.InvalidResponse(
                "Помилка API: ${response.code()}"
            )
        }

        val dtoList =
            response.body()
                ?.data
                ?.list
                ?: throw CryptoSyncException.InvalidResponse(
                    "API не повернув список криптовалют"
                )

        if (dtoList.size < 30) {
            throw CryptoSyncException.InvalidResponse(
                "API повернув менше 30 криптовалют"
            )
        }

        val top30 =
            dtoList.take(30)

        val updatedAt =
            System.currentTimeMillis()

        val entities =
            top30.mapIndexed { index, dto ->

                dto.toEntity(
                    rank = index + 1,
                    updatedAt = updatedAt
                )
                    ?: throw CryptoSyncException.InvalidResponse(
                        "У відповіді відсутні обов'язкові дані"
                    )
            }

        val uniqueIds =
            entities
                .map { it.id }
                .toSet()

        if (uniqueIds.size != 30) {
            throw CryptoSyncException.InvalidResponse(
                "API повернув дублікати криптовалют"
            )
        }

        dao.replaceAll(entities)
    }
}