package edu.utcj.acceso.data.sync

import edu.utcj.acceso.data.local.SyncQueueDao
import edu.utcj.acceso.data.local.SyncQueueEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deduplicating sync queue keyed by [syncKey] (INSERT OR IGNORE).
 */
@Singleton
class SyncQueue @Inject constructor(
    private val dao: SyncQueueDao
) {
    suspend fun enqueue(syncKey: String, entityType: String, payloadJson: String): Boolean {
        if (dao.exists(syncKey) > 0) return false // already queued — no duplicate
        val inserted = dao.enqueue(
            SyncQueueEntity(
                syncKey = syncKey,
                entityType = entityType,
                payloadJson = payloadJson
            )
        )
        return inserted != -1L
    }

    suspend fun pending(): List<SyncQueueEntity> = dao.pending()

    suspend fun remove(syncKey: String) = dao.remove(syncKey)

    suspend fun bumpAttempts(syncKey: String) = dao.bumpAttempts(syncKey)

    fun observePendingCount(): Flow<Int> = dao.observeCount()
}
