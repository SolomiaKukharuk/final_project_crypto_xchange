package com.example.cryptoexchange.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cryptoexchange.domain.model.SyncProgress
import com.example.cryptoexchange.domain.model.SyncStatus
import com.example.cryptoexchange.domain.repository.CryptoSyncManager
import com.example.cryptoexchange.domain.usecase.ObserveCryptocurrenciesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class CryptoViewModel(
    observeCryptocurrenciesUseCase: ObserveCryptocurrenciesUseCase,
    private val syncManager: CryptoSyncManager
) : ViewModel() {

    private val isStartingSync = MutableStateFlow(false)
    private val pendingManualSyncStartedAt = MutableStateFlow<Long?>(null)
    private val hasObservedCache = MutableStateFlow(false)

    private val syncProgressState: StateFlow<SyncProgress> =
        syncManager
            .observeManualSyncProgress()
            .onEach { progress ->
                val pendingStartedAt =
                    pendingManualSyncStartedAt.value

                val observedStartedAt =
                    progress.startedAt

                if (
                    pendingStartedAt != null &&
                    observedStartedAt != null &&
                    observedStartedAt >= pendingStartedAt
                ) {
                    isStartingSync.value = false
                    pendingManualSyncStartedAt.value = null
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SyncProgress()
            )

    val uiState: StateFlow<CryptoUiState> =
        combine(
            observeCryptocurrenciesUseCase()
                .onEach {
                    hasObservedCache.value = true
                },
            syncProgressState,
            isStartingSync,
            hasObservedCache
        ) { currencies, syncProgress, starting, hasLoadedCache ->

            val maxUpdatedInDb =
                currencies.maxOfOrNull { it.updatedAt }

            /*
             * Якщо manual sync колись завершився помилкою,
             * але після цього periodic worker успішно оновив Room,
             * стара помилка більше не повинна висіти в UI.
             */
            val failedSyncIsStale =
                syncProgress.status == SyncStatus.FAILED &&
                        syncProgress.startedAt != null &&
                        maxUpdatedInDb != null &&
                        maxUpdatedInDb > syncProgress.startedAt

            val effectiveSyncProgress =
                if (failedSyncIsStale) {
                    SyncProgress()
                } else {
                    syncProgress
                }

            val isManualQueuedOrRunning =
                effectiveSyncProgress.status == SyncStatus.ENQUEUED ||
                        effectiveSyncProgress.status == SyncStatus.RUNNING

            val isManualRunning =
                effectiveSyncProgress.status == SyncStatus.RUNNING

            val resolvedLoading =
                !hasLoadedCache ||
                        isManualRunning ||
                        starting

            val resolvedError =
                when (effectiveSyncProgress.status) {
                    SyncStatus.FAILED ->
                        effectiveSyncProgress.errorMessage
                            ?: "Помилка синхронізації"

                    else -> null
                }

            CryptoUiState(
                currencies = currencies,
                isLoading = resolvedLoading,
                syncState = effectiveSyncProgress.status,
                lastUpdatedAtMillis = maxUpdatedInDb,
                errorMessage = resolvedError,
                isManualSyncActive =
                    isManualQueuedOrRunning || starting
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CryptoUiState(
                    isLoading = true
                )
            )

    fun onRefreshClicked() {
        if (
            uiState.value.isLoading ||
            uiState.value.isManualSyncActive
        ) {
            return
        }

        isStartingSync.update { true }

        try {
            val startedAt =
                syncManager.scheduleManualSync()

            pendingManualSyncStartedAt.value =
                startedAt

            /*
             * Якщо WorkManager уже встиг передати новий стан
             * до моменту повернення scheduleManualSync().
             */
            val currentProgress =
                syncProgressState.value

            val observedStartedAt =
                currentProgress.startedAt

            if (
                observedStartedAt != null &&
                observedStartedAt >= startedAt
            ) {
                isStartingSync.value = false
                pendingManualSyncStartedAt.value = null
            }
        } catch (_: Exception) {
            isStartingSync.value = false
            pendingManualSyncStartedAt.value = null
        }
    }

    class Factory(
        private val observeCryptocurrenciesUseCase:
        ObserveCryptocurrenciesUseCase,
        private val syncManager: CryptoSyncManager
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            require(
                modelClass.isAssignableFrom(
                    CryptoViewModel::class.java
                )
            ) {
                "Unknown ViewModel class: ${modelClass.name}"
            }

            return CryptoViewModel(
                observeCryptocurrenciesUseCase =
                    observeCryptocurrenciesUseCase,
                syncManager = syncManager
            ) as T
        }
    }
}