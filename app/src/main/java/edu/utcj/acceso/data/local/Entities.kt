package edu.utcj.acceso.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.domain.model.StudentStatus

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val matricula: String,
    val nombre: String,
    val carrera: String = "",
    val status: StudentStatus = StudentStatus.PENDING,
    val consentVersion: String = "",
    val consentTimestampMs: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis(),
    val approvedAtMs: Long? = null,
    val approvedByGuard: String? = null,
    /** Correo opcional que el alumno capturó en su registro. */
    val correo: String? = null,
    /**
     * Llave pública EC P-256 (X.509, Base64) del teléfono del alumno. Con ella el guardia
     * verifica la firma de cada QR de acceso. La llave privada nunca sale del teléfono.
     */
    val publicKey: String? = null
)

/**
 * Nonces de QR de acceso ya presentados (anti-repetición). Se guardan hasta que el QR vence
 * (más la tolerancia de reloj) y luego se depuran.
 */
@Entity(tableName = "used_nonces", indices = [Index("expiresAtMs")])
data class UsedNonceEntity(
    @PrimaryKey val nonce: String,
    val matricula: String,
    val expiresAtMs: Long
)

@Entity(
    tableName = "access_events",
    indices = [Index("datetimeMs"), Index("matricula"), Index("synced"), Index(value = ["syncKey"], unique = true)]
)
data class AccessEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datetimeMs: Long = System.currentTimeMillis(),
    val matricula: String,
    val nombre: String,
    val result: AccessResult,
    val method: AccessMethod,
    val authorizingGuard: String? = null,
    val reason: String? = null,
    val similarity: Float? = null,
    val verifyDurationMs: Long? = null,
    val synced: Boolean = false,
    val syncKey: String = ""
)

@Entity(tableName = "visitors")
data class VisitorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val motivo: String,
    val visitaA: String,
    val entradaMs: Long = System.currentTimeMillis(),
    val salidaMs: Long? = null,
    val registeredByGuard: String
)

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datetimeMs: Long = System.currentTimeMillis(),
    val category: IncidentCategory,
    val description: String,
    val photoUri: String? = null,
    val reportedByGuard: String,
    val relatedMatricula: String? = null
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val syncKey: String,
    val entityType: String,
    val payloadJson: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)

@Entity(tableName = "eval_results")
data class EvalResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenario: String,
    val passed: Boolean,
    val notes: String = "",
    val similarity: Float? = null,
    val durationMs: Long? = null,
    val recordedAtMs: Long = System.currentTimeMillis(),
    val recordedBy: String = ""
)
