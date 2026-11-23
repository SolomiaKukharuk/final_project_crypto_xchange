package com.example.cryptoexchange.data.worker

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.cryptoexchange.domain.model.SyncProgress
import com.example.cryptoexchange.domain.model.SyncStatus
import com.example.cryptoexchange.domain.repository.CryptoSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkManagerCryptoSyncManager(
    private val workManager: WorkManager,
    private val syncScheduler: CryptoSyncScheduler
) : CryptoSyncManager {

    override fun observeManualSyncProgress(): Flow<SyncProgress> {
        return workManager
            .getWorkInfosForUniqueWorkFlow(
                CryptoSyncScheduler.MANUAL_SYNC_WORK_NAME
            )
            .map { workInfos ->
                val workInfo = selectLatestManualWork(workInfos)

                workInfo?.toSyncProgress()
                    ?: SyncProgress()
            }
    }

    override fun scheduleManualSync(): Long {
        return syncScheduler.scheduleManualSync()
    }

    private fun selectLatestManualWork(
        workInfos: List<WorkInfo>
    ): WorkInfo? {
        if (workInfos.isEmpty()) {
            return null
        }

        val taggedWorks = workInfos.mapNotNull { workInfo ->
            val startedAt = getStartedAt(workInfo)

            if (startedAt != null) {
                startedAt to workInfo
            } else {
                null
            }
        }

        if (taggedWorks.isNotEmpty()) {
            return taggedWorks
                .maxByOrNull { it.first }
                ?.second
        }

        return workInfos.firstOrNull {
            it.state == WorkInfo.State.RUNNING
        } ?: workInfos.firstOrNull {
            it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.BLOCKED
        } ?: workInfos.lastOrNull()
    }

    private fun WorkInfo.toSyncProgress(): SyncProgress {
        val startedAt = getStartedAt(this)

        return when (state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> {
                SyncProgress(
                    status = SyncStatus.ENQUEUED,
                    startedAt = startedAt
                )
            }

            WorkInfo.State.RUNNING -> {
                SyncProgress(
                    status = SyncStatus.RUNNING,
                    startedAt = startedAt
                )
            }

            WorkInfo.State.SUCCEEDED -> {
                SyncProgress(
                    status = SyncStatus.SUCCEEDED,
                    startedAt = startedAt
                )
            }

            WorkInfo.State.FAILED -> {
                val message =
                    outputData.getString(
                        CryptoSyncWorker.OUTPUT_MESSAGE
                    )
                        ?.takeIf { it.isNotBlank() }
                        ?: "Не вдалося оновити дані"

                SyncProgress(
                    status = SyncStatus.FAILED,
                    errorMessage = message,
                    startedAt = startedAt
                )
            }

            WorkInfo.State.CANCELLED -> {
                SyncProgress(
                    status = SyncStatus.IDLE,
                    startedAt = startedAt
                )
            }
        }
    }

    private fun getStartedAt(
        workInfo: WorkInfo
    ): Long? {
        val tag = workInfo.tags.firstOrNull {
            it.startsWith(
                CryptoSyncScheduler.MANUAL_SYNC_TAG_PREFIX
            )
        }

        return tag
            ?.removePrefix(
                CryptoSyncScheduler.MANUAL_SYNC_TAG_PREFIX
            )
            ?.toLongOrNull()
    }
}