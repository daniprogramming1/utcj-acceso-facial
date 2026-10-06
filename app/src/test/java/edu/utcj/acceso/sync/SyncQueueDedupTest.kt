package edu.utcj.acceso.sync

import edu.utcj.acceso.data.local.SyncQueueDao
import edu.utcj.acceso.data.local.SyncQueueEntity
import edu.utcj.acceso.data.sync.SyncQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueba la clase real [SyncQueue] con un DAO falso que imita INSERT OR IGNORE. */
class SyncQueueDedupTest {

    private class FakeDao : SyncQueueDao {
        val rows = linkedMapOf<String, SyncQueueEntity>()
        private val count = MutableStateFlow(0)
        override suspend fun enqueue(item: SyncQueueEntity): Long {
            if (rows.containsKey(item.syncKey)) return -1L
            rows[item.syncKey] = item; count.value = rows.size
            return rows.size.toLong()
        }
        override suspend fun pending() = rows.values.sortedBy { it.createdAtMs }
        override suspend fun remove(key: String) { rows.remove(key); count.value = rows.size }
        override fun observeCount(): Flow<Int> = count
        override suspend fun exists(key: String) = if (rows.containsKey(key)) 1 else 0
        override suspend fun bumpAttempts(key: String) {
            rows[key]?.let { rows[key] = it.copy(attempts = it.attempts + 1) }
        }
    }

    @Test
    fun duplicateKey_notEnqueuedTwice() = runBlocking {
        val q = SyncQueue(FakeDao())
        assertTrue(q.enqueue("evt-1", "access_event", "{}"))
        assertFalse(q.enqueue("evt-1", "access_event", "{}"))
        assertEquals(1, q.pending().size)
    }

    @Test
    fun differentKeys_bothKept() = runBlocking {
        val q = SyncQueue(FakeDao())
        assertTrue(q.enqueue("a", "access_event", "{}"))
        assertTrue(q.enqueue("b", "access_event", "{}"))
        assertEquals(2, q.pending().size)
    }

    @Test
    fun remove_allowsReenqueue() = runBlocking {
        val q = SyncQueue(FakeDao())
        q.enqueue("x", "access_event", "{}")
        q.remove("x")
        assertTrue(q.pending().isEmpty())
        assertTrue(q.enqueue("x", "access_event", "{}"))
    }

    @Test
    fun retryBumpsAttempts_withoutDuplicating() = runBlocking {
        val dao = FakeDao()
        val q = SyncQueue(dao)
        q.enqueue("r", "access_event", "{}")
        q.bumpAttempts("r"); q.bumpAttempts("r")
        assertEquals(1, q.pending().size)
        assertEquals(2, q.pending().first().attempts)
    }
}
