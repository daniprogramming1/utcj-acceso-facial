package edu.utcj.acceso.data.qr

import edu.utcj.acceso.data.local.UsedNonceDao
import edu.utcj.acceso.data.local.UsedNonceEntity
import edu.utcj.acceso.data.repository.AccessLogRepository
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.qr.AccessDecider
import edu.utcj.acceso.domain.qr.AccessDecision
import edu.utcj.acceso.domain.qr.AccessToken
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.domain.qr.GuardPolicy
import edu.utcj.acceso.domain.qr.QrCodec
import edu.utcj.acceso.domain.qr.QrParseResult
import edu.utcj.acceso.domain.qr.RegistrationCheck
import edu.utcj.acceso.domain.qr.RegistrationDecider
import edu.utcj.acceso.domain.qr.StudentRecord
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que el escáner del guardia leyó. */
sealed class ScanOutcome {
    abstract val atMs: Long

    /** QR de acceso: decisión + datos del alumno (si se conoce). */
    data class Access(
        val decision: AccessDecision,
        val token: AccessToken,
        override val atMs: Long,
        val durationMs: Long
    ) : ScanOutcome() {
        val allowed: Boolean get() = decision is AccessDecision.Allowed
        val reason: DenialReason? get() = (decision as? AccessDecision.Denied)?.reason
        val student: StudentRecord? get() = decision.student
    }

    /** QR de registro (solo se aprueba desde el panel). */
    data class Registration(val check: RegistrationCheck, override val atMs: Long) : ScanOutcome()

    /** Código ajeno o dañado. */
    data class Invalid(override val atMs: Long) : ScanOutcome()
}

/**
 * Verificación de QR en el teléfono del guardia (sin conexión): firma con la llave del alumno,
 * vigencia (±30 s de tolerancia), vigencia máxima, nonce de un solo uso, estatus y horario.
 */
@Singleton
class QrAccessService @Inject constructor(
    private val students: StudentRepository,
    private val nonces: UsedNonceDao,
    private val settings: SettingsRepository,
    private val accessLog: AccessLogRepository
) {
    /** Para registrar en bitácora aunque la pantalla que escaneó ya se haya cerrado. */
    private val detached = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun policy() = GuardPolicy(maxLifetimeSec = settings.getGuardMaxQrValiditySec())

    suspend fun scan(raw: String, nowMs: Long = System.currentTimeMillis()): ScanOutcome {
        val started = System.nanoTime()
        return when (val parsed = QrCodec.parse(raw)) {
            is QrParseResult.Invalid -> ScanOutcome.Invalid(nowMs)
            is QrParseResult.Registration ->
                ScanOutcome.Registration(RegistrationDecider.check(parsed, students.record(parsed.payload.matricula)), nowMs)
            is QrParseResult.Access -> {
                val policy = policy()
                nonces.deleteExpired(nowMs)
                val student = students.record(parsed.token.matricula)
                val withinHours = TimeUtil.isWithinHours(nowMs, settings.getHoursStart(), settings.getHoursEnd())
                var decision = AccessDecider.decide(
                    parsed, student, policy, nowMs,
                    nonceAlreadyUsed = nonces.exists(parsed.token.nonce) > 0,
                    withinHours = withinHours
                )
                if (decision.consumesNonce) {
                    val keepUntil = parsed.token.expiresAtMs + policy.clockSkewSec * 1000L
                    val inserted = nonces.tryInsert(UsedNonceEntity(parsed.token.nonce, parsed.token.matricula, keepUntil))
                    // Dos lecturas simultáneas del mismo QR: solo una gana.
                    if (inserted == -1L) decision = AccessDecision.Denied(DenialReason.REUSED, decision.student)
                }
                ScanOutcome.Access(decision, parsed.token, nowMs, (System.nanoTime() - started) / 1_000_000)
            }
        }
    }

    /** Registra en bitácora el resultado de un QR de acceso. */
    suspend fun log(outcome: ScanOutcome.Access, guard: String, result: AccessResult, reason: String? = outcome.reason?.labelEs) {
        val s = outcome.student
        accessLog.log(
            matricula = s?.matricula ?: outcome.token.matricula,
            nombre = s?.nombre ?: "No registrado",
            result = result,
            method = AccessMethod.QR,
            guard = guard,
            reason = reason,
            durationMs = outcome.durationMs
        )
    }

    fun logDetached(outcome: ScanOutcome.Access, guard: String, result: AccessResult, reason: String?) {
        detached.launch { runCatching { log(outcome, guard, result, reason) } }
    }

    /** Registra un código ilegible o un QR de registro mostrado en el lugar equivocado. */
    suspend fun logRejectedCode(guard: String, reason: DenialReason, matricula: String? = null, nombre: String? = null) {
        accessLog.log(
            matricula = matricula ?: "—",
            nombre = nombre ?: "Código no válido",
            result = AccessResult.DENIED,
            method = AccessMethod.QR,
            guard = guard,
            reason = reason.labelEs
        )
    }
}

/**
 * Evita procesar varias veces el mismo código mientras sigue frente a la cámara: el mismo
 * texto se ignora hasta que deja de verse durante [quietMs].
 */
class ScanDebouncer(private val quietMs: Long = 4_000L) {
    private var last: String? = null
    private var lastSeenMs = 0L

    /** @return `true` si el código debe procesarse. */
    fun accept(raw: String, nowMs: Long): Boolean {
        val repeated = raw == last && nowMs - lastSeenMs < quietMs
        last = raw
        lastSeenMs = nowMs
        return !repeated
    }
}
