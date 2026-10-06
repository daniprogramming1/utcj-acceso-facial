package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.local.VisitorDao
import edu.utcj.acceso.data.local.VisitorEntity
import edu.utcj.acceso.domain.model.Visitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitorRepository @Inject constructor(
    private val dao: VisitorDao
) {
    fun observeAll(): Flow<List<Visitor>> = dao.observeAll().map { it.map(::toDomain) }
    fun observeInside(): Flow<List<Visitor>> = dao.observeInside().map { it.map(::toDomain) }

    suspend fun register(nombre: String, motivo: String, visitaA: String, guard: String): Long =
        dao.insert(
            VisitorEntity(
                nombre = nombre,
                motivo = motivo,
                visitaA = visitaA,
                registeredByGuard = guard
            )
        )

    suspend fun markExit(id: Long) = dao.markExit(id, System.currentTimeMillis())

    private fun toDomain(e: VisitorEntity) = Visitor(
        id = e.id,
        nombre = e.nombre,
        motivo = e.motivo,
        visitaA = e.visitaA,
        entradaMs = e.entradaMs,
        salidaMs = e.salidaMs,
        registeredByGuard = e.registeredByGuard
    )
}
