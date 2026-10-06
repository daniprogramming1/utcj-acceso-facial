package edu.utcj.acceso.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import edu.utcj.acceso.data.local.AccessEventDao
import edu.utcj.acceso.data.remote.FirestoreDataSource
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueue: SyncQueue,
    private val accessEventDao: AccessEventDao,
    private val firestore: FirestoreDataSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val items = syncQueue.pending()
        var failures = 0
        for (item in items) {
            try {
                when (item.entityType) {
                    TYPE_ACCESS -> {
                        val event = accessEventDao.findBySyncKey(item.syncKey)
                        if (event != null && event.synced) {
                            // Ya sincronizado: nunca se sube dos veces
                            syncQueue.remove(item.syncKey)
                        } else if (event != null) {
                            val ok = firestore.uploadAccessEvent(event)
                            if (ok) {
                                accessEventDao.markSynced(event.id)
                                syncQueue.remove(item.syncKey)
                            } else {
                                syncQueue.bumpAttempts(item.syncKey)
                                failures++
                            }
                        } else {
                            syncQueue.remove(item.syncKey)
                        }
                    }
                    else -> syncQueue.remove(item.syncKey)
                }
            } catch (e: Exception) {
                syncQueue.bumpAttempts(item.syncKey)
                failures++
            }
        }
        return if (failures == 0) Result.success() else Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "utcj_acceso_sync"
        const val PERIODIC_NAME = "utcj_acceso_sync_periodic"
        const val TYPE_ACCESS = "access_event"

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Sincronización inmediata (única: KEEP evita trabajos duplicados en cola). */
        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                req
            )
        }

        /** Respaldo periódico cada 15 min (mínimo permitido por WorkManager). */
        fun schedulePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
