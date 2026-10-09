package edu.utcj.acceso.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.StudentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY nombre ASC")
    fun observeAll(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE status = :status ORDER BY createdAtMs DESC")
    fun observeByStatus(status: StudentStatus): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE matricula = :matricula LIMIT 1")
    suspend fun getByMatricula(matricula: String): StudentEntity?

    @Query("SELECT * FROM students WHERE matricula = :matricula LIMIT 1")
    fun observeByMatricula(matricula: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE nombre LIKE '%' || :q || '%' OR matricula LIKE '%' || :q || '%' ORDER BY nombre")
    suspend fun search(q: String): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(student: StudentEntity)

    @Update
    suspend fun update(student: StudentEntity)

    @Query("DELETE FROM students WHERE matricula = :matricula")
    suspend fun delete(matricula: String)

    @Query("SELECT * FROM students WHERE status IN ('APPROVED','ACTIVO')")
    suspend fun getApproved(): List<StudentEntity>
}

@Dao
interface UsedNonceDao {
    /** Marca el nonce como usado. Devuelve -1 si ya existía (operación atómica en SQLite). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tryInsert(entity: UsedNonceEntity): Long

    @Query("SELECT COUNT(*) FROM used_nonces WHERE nonce = :nonce")
    suspend fun exists(nonce: String): Int

    @Query("DELETE FROM used_nonces WHERE expiresAtMs < :nowMs")
    suspend fun deleteExpired(nowMs: Long): Int
}

@Dao
interface AccessEventDao {
    @Insert
    suspend fun insert(event: AccessEventEntity): Long

    @Query("SELECT * FROM access_events ORDER BY datetimeMs DESC")
    fun observeAll(): Flow<List<AccessEventEntity>>

    @Query(
        """
        SELECT * FROM access_events
        WHERE (:fromMs IS NULL OR datetimeMs >= :fromMs)
          AND (:toMs IS NULL OR datetimeMs <= :toMs)
          AND (:matricula IS NULL OR matricula = :matricula)
          AND (:result IS NULL OR result = :result)
        ORDER BY datetimeMs DESC
        """
    )
    fun observeFiltered(
        fromMs: Long?,
        toMs: Long?,
        matricula: String?,
        result: AccessResult?
    ): Flow<List<AccessEventEntity>>

    @Query("SELECT * FROM access_events WHERE synced = 0")
    suspend fun getUnsynced(): List<AccessEventEntity>

    @Query("UPDATE access_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM access_events WHERE syncKey = :key LIMIT 1")
    suspend fun findBySyncKey(key: String): AccessEventEntity?

    @Query("SELECT * FROM access_events WHERE datetimeMs >= :fromMs ORDER BY datetimeMs ASC")
    suspend fun getSince(fromMs: Long): List<AccessEventEntity>

    @Query(
        """
        SELECT * FROM access_events
        WHERE matricula = :matricula AND result = 'DENIED'
        ORDER BY datetimeMs DESC LIMIT :limit
        """
    )
    suspend fun recentDenials(matricula: String, limit: Int): List<AccessEventEntity>
}

@Dao
interface VisitorDao {
    @Insert
    suspend fun insert(v: VisitorEntity): Long

    @Query("SELECT * FROM visitors ORDER BY entradaMs DESC")
    fun observeAll(): Flow<List<VisitorEntity>>

    @Query("SELECT * FROM visitors WHERE salidaMs IS NULL ORDER BY entradaMs DESC")
    fun observeInside(): Flow<List<VisitorEntity>>

    @Query("UPDATE visitors SET salidaMs = :outMs WHERE id = :id")
    suspend fun markExit(id: Long, outMs: Long)
}

@Dao
interface IncidentDao {
    @Insert
    suspend fun insert(i: IncidentEntity): Long

    @Query("SELECT * FROM incidents ORDER BY datetimeMs DESC")
    fun observeAll(): Flow<List<IncidentEntity>>
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAtMs ASC")
    suspend fun pending(): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE syncKey = :key")
    suspend fun remove(key: String)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncKey = :key")
    suspend fun exists(key: String): Int

    @Query("UPDATE sync_queue SET attempts = attempts + 1 WHERE syncKey = :key")
    suspend fun bumpAttempts(key: String)
}

@Dao
interface EvalResultDao {
    @Insert
    suspend fun insert(e: EvalResultEntity): Long

    @Query("SELECT * FROM eval_results ORDER BY recordedAtMs DESC")
    fun observeAll(): Flow<List<EvalResultEntity>>
}
