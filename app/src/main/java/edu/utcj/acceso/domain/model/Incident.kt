package edu.utcj.acceso.domain.model

data class Incident(
    val id: Long = 0,
    val datetimeMs: Long = System.currentTimeMillis(),
    val category: IncidentCategory,
    val description: String,
    val photoUri: String? = null,
    val reportedByGuard: String,
    val relatedMatricula: String? = null
)

enum class IncidentCategory {
    SEGURIDAD,
    ACCESO_NO_AUTORIZADO,
    COMPORTAMIENTO,
    TECNICO,
    OTRO
}
