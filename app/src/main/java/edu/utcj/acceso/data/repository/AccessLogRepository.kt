package edu.utcj.acceso.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.utcj.acceso.data.local.AccessEventDao
import edu.utcj.acceso.data.local.AccessEventEntity
import edu.utcj.acceso.data.export.PdfReportBuilder
import edu.utcj.acceso.data.sync.SyncQueue
import edu.utcj.acceso.data.sync.SyncWorker
import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessLogRepository @Inject constructor(
    private val dao: AccessEventDao,
    private val syncQueue: SyncQueue,
    @ApplicationContext private val context: Context
) {
    fun observeAll(): Flow<List<AccessEvent>> = dao.observeAll().map { it.map(::toDomain) }

    fun observeFiltered(
        fromMs: Long?,
        toMs: Long?,
        matricula: String?,
        result: AccessResult?
    ): Flow<List<AccessEvent>> =
        dao.observeFiltered(fromMs, toMs, matricula, result).map { it.map(::toDomain) }

    suspend fun log(
        matricula: String,
        nombre: String,
        result: AccessResult,
        method: AccessMethod,
        guard: String? = null,
        reason: String? = null,
        similarity: Float? = null,
        durationMs: Long? = null
    ): AccessEvent {
        val syncKey = UUID.randomUUID().toString()
        val entity = AccessEventEntity(
            datetimeMs = System.currentTimeMillis(),
            matricula = matricula,
            nombre = nombre,
            result = result,
            method = method,
            authorizingGuard = guard,
            reason = reason,
            similarity = similarity,
            verifyDurationMs = durationMs,
            synced = false,
            syncKey = syncKey
        )
        val id = dao.insert(entity)
        val payload = JSONObject()
            .put("id", id)
            .put("syncKey", syncKey)
            .toString()
        syncQueue.enqueue(syncKey, SyncWorker.TYPE_ACCESS, payload)
        SyncWorker.enqueue(context)
        return toDomain(entity.copy(id = id))
    }

    suspend fun recentConsecutiveDenials(matricula: String, limit: Int = 3): Int {
        val list = dao.recentDenials(matricula, limit)
        return list.size
    }

    suspend fun since(fromMs: Long): List<AccessEvent> = dao.getSince(fromMs).map(::toDomain)

    fun observeSince(fromMs: Long): Flow<List<AccessEvent>> = observeFiltered(fromMs, null, null, null)

    suspend fun exportCsv(events: List<AccessEvent>): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "bitacora_${System.currentTimeMillis()}.csv")
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("\uFEFF") // BOM: Excel abre correctamente acentos
            w.appendLine("fecha,matricula,nombre,resultado,metodo,guardia,motivo,similitud,duracion_ms")
            events.forEach { e ->
                w.appendLine(
                    listOf(
                        TimeUtil.formatDateTime(e.datetimeMs),
                        e.matricula,
                        csv(e.nombre),
                        e.result.name,
                        e.method.name,
                        csv(e.authorizingGuard.orEmpty()),
                        csv(e.reason.orEmpty()),
                        e.similarity?.toString().orEmpty(),
                        e.verifyDurationMs?.toString().orEmpty()
                    ).joinToString(",")
                )
            }
        }
        file
    }

    suspend fun exportPdf(events: List<AccessEvent>, rangeLabel: String = "Todos los registros"): File =
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "bitacora_${System.currentTimeMillis()}.pdf")
            PdfReportBuilder(rangeLabel = rangeLabel).write(events, file)
        }

    private fun csv(v: String) = "\"" + v.replace("\"", "\"\"") + "\""

    private fun toDomain(e: AccessEventEntity) = AccessEvent(
        id = e.id,
        datetimeMs = e.datetimeMs,
        matricula = e.matricula,
        nombre = e.nombre,
        result = e.result,
        method = e.method,
        authorizingGuard = e.authorizingGuard,
        reason = e.reason,
        similarity = e.similarity,
        verifyDurationMs = e.verifyDurationMs,
        synced = e.synced
    )
}
