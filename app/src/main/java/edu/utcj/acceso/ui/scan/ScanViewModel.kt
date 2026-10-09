package edu.utcj.acceso.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.qr.ExternalQrInput
import edu.utcj.acceso.data.qr.QrAccessService
import edu.utcj.acceso.data.qr.ScanDebouncer
import edu.utcj.acceso.data.qr.ScanOutcome
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.domain.qr.RegistrationCheck
import edu.utcj.acceso.domain.qr.RegistrationPayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resultado que el panel muestra junto a la cámara. */
sealed class PanelScanResult {
    /** QR de acceso. Si [awaitingGuard] el guardia debe tocar «Registrar entrada» o «No permitir». */
    data class Access(val card: ScanCardModel, val awaitingGuard: Boolean) : PanelScanResult()

    /** QR de registro de un alumno. */
    data class Registration(
        val payload: RegistrationPayload?,
        val kind: Kind,
        val previousStatus: StudentStatus? = null,
        val replacesKey: Boolean = false
    ) : PanelScanResult() {
        enum class Kind { READY, BLOCKED, INVALID }
    }
}

data class ScanUi(
    val result: PanelScanResult? = null,
    val busy: Boolean = false,
    val working: Boolean = false
)

/**
 * Escáner del panel del guardia (cámara trasera). Lee tanto QR de acceso como QR de registro.
 *
 * Bitácora: un QR rechazado se registra al momento; uno válido se registra como permitido cuando
 * el guardia toca «Registrar entrada» o como denegado si toca «No permitir» (o si sale del panel
 * sin decidir). El nonce se consume al escanear, así que el QR ya no puede reutilizarse.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val qr: QrAccessService,
    private val external: ExternalQrInput,
    private val students: StudentRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(ScanUi())
    val ui: StateFlow<ScanUi> = _ui.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val debouncer = ScanDebouncer()
    private var externalJob: kotlinx.coroutines.Job? = null

    /** La pantalla está visible: acepta también códigos de [ExternalQrInput]. */
    fun setActive(active: Boolean) {
        externalJob?.cancel()
        externalJob = if (active) viewModelScope.launch { external.codes.collect { onCode(it) } } else null
    }
    private var pendingAccess: ScanOutcome.Access? = null
    private var pendingRegistration: RegistrationPayload? = null

    private val guard: String get() = auth.currentGuardName()

    fun onCode(raw: String) {
        val now = System.currentTimeMillis()
        if (!debouncer.accept(raw, now) || _ui.value.busy || _ui.value.result != null) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = try {
                when (val o = qr.scan(raw, now)) {
                    is ScanOutcome.Access -> {
                        if (o.allowed) {
                            pendingAccess = o
                        } else {
                            qr.log(o, guard, AccessResult.DENIED)
                        }
                        PanelScanResult.Access(o.toCard(), awaitingGuard = o.allowed)
                    }
                    is ScanOutcome.Registration -> registrationResult(o.check)
                    is ScanOutcome.Invalid -> {
                        qr.logRejectedCode(guard, DenialReason.INVALID_QR)
                        PanelScanResult.Access(ScanCardModel.denied(DenialReason.INVALID_QR, now), awaitingGuard = false)
                    }
                }
            } catch (e: Exception) {
                PanelScanResult.Access(ScanCardModel.denied(DenialReason.INVALID_QR, now), awaitingGuard = false)
            }
            _ui.update { it.copy(busy = false, result = result) }
        }
    }

    private fun registrationResult(c: RegistrationCheck): PanelScanResult.Registration = when (c) {
        is RegistrationCheck.Ok -> {
            pendingRegistration = c.payload
            PanelScanResult.Registration(c.payload, PanelScanResult.Registration.Kind.READY, c.previousStatus, c.replacesKey)
        }
        is RegistrationCheck.Blocked -> PanelScanResult.Registration(c.payload, PanelScanResult.Registration.Kind.BLOCKED, c.status)
        is RegistrationCheck.Invalid -> PanelScanResult.Registration(c.payload, PanelScanResult.Registration.Kind.INVALID)
    }

    /** El guardia confirma la entrada del alumno (QR válido). */
    fun registerEntry() {
        val o = pendingAccess ?: return
        pendingAccess = null
        _ui.update { it.copy(working = true) }
        viewModelScope.launch {
            qr.log(o, guard, AccessResult.ALLOWED)
            _messages.tryEmit("Entrada registrada: ${o.student?.nombre ?: o.token.matricula}")
            _ui.update { ScanUi() }
        }
    }

    /** El guardia decide no permitir el acceso pese a que el QR es válido. */
    fun denyEntry() {
        val o = pendingAccess ?: return
        pendingAccess = null
        viewModelScope.launch {
            qr.log(o, guard, AccessResult.DENIED, DenialReason.DENIED_BY_GUARD.labelEs)
            _messages.tryEmit("Acceso no permitido · registrado en bitácora")
            _ui.update { ScanUi() }
        }
    }

    fun approveRegistration() = decideRegistration(approve = true)
    fun rejectRegistration() = decideRegistration(approve = false)

    private fun decideRegistration(approve: Boolean) {
        val p = pendingRegistration ?: return
        pendingRegistration = null
        _ui.update { it.copy(working = true) }
        viewModelScope.launch {
            students.approveFromRegistration(p, guard, approve)
            _messages.tryEmit(if (approve) "${p.nombre} ya puede entrar con su QR" else "Registro de ${p.nombre} rechazado")
            _ui.update { ScanUi() }
        }
    }

    /** Cierra el resultado y vuelve a escanear. */
    fun dismiss() {
        if (pendingAccess != null) return denyEntry()
        pendingRegistration = null
        _ui.update { ScanUi() }
    }

    override fun onCleared() {
        // Salió del panel sin decidir: queda constancia en la bitácora.
        pendingAccess?.let { qr.logDetached(it, guard, AccessResult.DENIED, "Sin confirmar por el guardia") }
        pendingAccess = null
    }
}
