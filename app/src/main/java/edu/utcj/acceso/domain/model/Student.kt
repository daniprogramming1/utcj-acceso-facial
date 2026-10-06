package edu.utcj.acceso.domain.model

/**
 * Student registered for facial access.
 * Photos are NEVER stored — only encrypted embeddings in local DB.
 */
data class Student(
    val matricula: String,
    val nombre: String,
    val carrera: String = "",
    val status: StudentStatus = StudentStatus.PENDING,
    val consentVersion: String = "",
    val consentTimestampMs: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis(),
    val approvedAtMs: Long? = null,
    val approvedByGuard: String? = null
)

enum class StudentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    BAJA,
    SUSPENDIDO,
    ACTIVO;

    fun allowsAccess(): Boolean = this == APPROVED || this == ACTIVO

    companion object {
        fun fromCsv(raw: String): StudentStatus = when (raw.trim().uppercase()) {
            "ACTIVO", "ACTIVE" -> ACTIVO
            "BAJA" -> BAJA
            "SUSPENDIDO", "SUSPENDED" -> SUSPENDIDO
            "PENDIENTE", "PENDING" -> PENDING
            "APROBADO", "APPROVED" -> APPROVED
            "RECHAZADO", "REJECTED" -> REJECTED
            else -> PENDING
        }
    }
}
