package edu.utcj.acceso.domain.qr

import edu.utcj.acceso.domain.model.StudentStatus

/** Motivos de rechazo de un QR, con el texto que verá el guardia / alumno. */
enum class DenialReason(val labelEs: String, val detailEs: String) {
    INVALID_QR("QR no válido", "El código no pertenece a Acceso UTCJ o está dañado."),
    REGISTRATION_QR("QR de registro", "Este es el QR de registro. El guardia debe escanearlo en Aprobaciones."),
    NOT_REGISTERED("Alumno no registrado", "La matrícula no está dada de alta en este equipo de seguridad."),
    NO_KEY("Sin QR de registro", "El alumno aún no presenta su QR de registro en caseta."),
    BAD_SIGNATURE("Firma inválida", "El QR no fue generado por el teléfono registrado del alumno."),
    NOT_YET_VALID("Hora del QR inválida", "La hora del teléfono del alumno está adelantada. Pide que la ajuste."),
    LIFETIME_TOO_LONG("Vigencia no permitida", "El QR dura más de lo permitido por seguridad. El alumno debe elegir una vigencia menor."),
    EXPIRED("QR vencido", "Pide al alumno que muestre su QR actualizado."),
    REUSED("QR ya usado", "Este QR ya se presentó antes. Cada QR solo sirve una vez."),
    BAJA("Alumno dado de baja", "Su estatus institucional no permite el acceso."),
    SUSPENDIDO("Alumno suspendido", "Su estatus institucional no permite el acceso."),
    PENDING("Pendiente de aprobación", "Su registro aún no ha sido aprobado por seguridad."),
    REJECTED("Registro rechazado", "Su registro fue rechazado por seguridad."),
    OUT_OF_HOURS("Fuera de horario", "El acceso está fuera del horario permitido."),
    DENIED_BY_GUARD("Denegado por el guardia", "El guardia decidió no permitir el acceso.")
}

/** Datos del alumno guardados en el teléfono del guardia. */
data class StudentRecord(
    val matricula: String,
    val nombre: String,
    val carrera: String,
    val status: StudentStatus,
    val publicKey: ByteArray?
)

/** Reglas del guardia para aceptar un QR de acceso. */
data class GuardPolicy(
    /** Vigencia máxima aceptada (el alumno no puede generar QR más largos). */
    val maxLifetimeSec: Int,
    /** Tolerancia por diferencia de relojes entre teléfonos. */
    val clockSkewSec: Int = DEFAULT_CLOCK_SKEW_SEC
) {
    companion object {
        const val DEFAULT_CLOCK_SKEW_SEC = 30
    }
}

sealed class AccessDecision {
    abstract val student: StudentRecord?

    /** `true` si la firma y la vigencia son válidas y el nonce debe marcarse como usado. */
    abstract val consumesNonce: Boolean

    data class Allowed(override val student: StudentRecord) : AccessDecision() {
        override val consumesNonce = true
    }

    data class Denied(
        val reason: DenialReason,
        override val student: StudentRecord?,
        override val consumesNonce: Boolean = false
    ) : AccessDecision()
}

/** Decisión pura (sin Android) de un QR de acceso; el orden de las reglas importa. */
object AccessDecider {
    fun decide(
        qr: QrParseResult.Access,
        student: StudentRecord?,
        policy: GuardPolicy,
        nowMs: Long,
        nonceAlreadyUsed: Boolean,
        withinHours: Boolean
    ): AccessDecision {
        val t = qr.token
        val skew = policy.clockSkewSec * 1000L
        if (student == null) return AccessDecision.Denied(DenialReason.NOT_REGISTERED, null)
        val key = student.publicKey ?: return AccessDecision.Denied(DenialReason.NO_KEY, student)
        if (!QrCrypto.verify(key, qr.signedPart.toByteArray(Charsets.UTF_8), qr.signature)) {
            return AccessDecision.Denied(DenialReason.BAD_SIGNATURE, student)
        }
        if (t.expiresAtMs <= t.issuedAtMs) return AccessDecision.Denied(DenialReason.INVALID_QR, student)
        if (t.issuedAtMs - skew > nowMs) return AccessDecision.Denied(DenialReason.NOT_YET_VALID, student)
        if (t.lifetimeMs > policy.maxLifetimeSec * 1000L) return AccessDecision.Denied(DenialReason.LIFETIME_TOO_LONG, student)
        if (nowMs - skew > t.expiresAtMs) return AccessDecision.Denied(DenialReason.EXPIRED, student)
        if (nonceAlreadyUsed) return AccessDecision.Denied(DenialReason.REUSED, student)
        // A partir de aquí el QR es auténtico y vigente: se consume aunque se niegue por estatus.
        val statusReason = when (student.status) {
            StudentStatus.BAJA -> DenialReason.BAJA
            StudentStatus.SUSPENDIDO -> DenialReason.SUSPENDIDO
            StudentStatus.PENDING -> DenialReason.PENDING
            StudentStatus.REJECTED -> DenialReason.REJECTED
            StudentStatus.APPROVED, StudentStatus.ACTIVO -> null
        }
        if (statusReason != null) return AccessDecision.Denied(statusReason, student, consumesNonce = true)
        if (!withinHours) return AccessDecision.Denied(DenialReason.OUT_OF_HOURS, student, consumesNonce = true)
        return AccessDecision.Allowed(student)
    }
}

/** Resultado de revisar un QR de registro en el teléfono del guardia. */
sealed class RegistrationCheck {
    abstract val payload: RegistrationPayload?

    data class Ok(override val payload: RegistrationPayload, val replacesKey: Boolean, val previousStatus: StudentStatus?) : RegistrationCheck()
    data class Blocked(override val payload: RegistrationPayload, val status: StudentStatus) : RegistrationCheck()
    data class Invalid(override val payload: RegistrationPayload? = null) : RegistrationCheck()
}

object RegistrationDecider {
    /** BAJA / SUSPENDIDO (CSV institucional) impiden aprobar; una firma inválida invalida el QR. */
    fun check(qr: QrParseResult.Registration, existing: StudentRecord?): RegistrationCheck {
        if (!QrCrypto.verifyRegistration(qr)) return RegistrationCheck.Invalid(qr.payload)
        val st = existing?.status
        if (st == StudentStatus.BAJA || st == StudentStatus.SUSPENDIDO) return RegistrationCheck.Blocked(qr.payload, st)
        val replaces = existing?.publicKey != null && !existing.publicKey.contentEquals(qr.payload.publicKey)
        return RegistrationCheck.Ok(qr.payload, replaces, st)
    }
}
