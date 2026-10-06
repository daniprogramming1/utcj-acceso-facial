package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.local.IncidentDao
import edu.utcj.acceso.data.local.IncidentEntity
import edu.utcj.acceso.domain.model.Incident
import edu.utcj.acceso.domain.model.IncidentCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val dao: IncidentDao
) {
    fun observeAll(): Flow<List<Incident>> = dao.observeAll().map { it.map(::toDomain) }

    suspend fun report(
        category: IncidentCategory,
        description: String,
        guard: String,
        photoUri: String? = null,
        matricula: String? = null
    ): Long = dao.insert(
        IncidentEntity(
            category = category,
            description = description,
            photoUri = photoUri,
            reportedByGuard = guard,
            relatedMatricula = matricula
        )
    )

    private fun toDomain(e: IncidentEntity) = Incident(
        id = e.id,
        datetimeMs = e.datetimeMs,
        category = e.category,
        description = e.description,
        photoUri = e.photoUri,
        reportedByGuard = e.reportedByGuard,
        relatedMatricula = e.relatedMatricula
    )
}
