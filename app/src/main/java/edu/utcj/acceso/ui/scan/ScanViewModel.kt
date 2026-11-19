package edu.utcj.acceso.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.qr.ExternalQrInput
import edu.utcj.acceso.data.qr.QrAccessService
import edu.utcj.acceso.data.qr.ScanDebouncer
import edu.utcj.acceso.data.qr.ScanOutcome
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.domain.qr.RegistrationCheck
import edu.utcj.acceso.domain.qr.RegistrationPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    /**
     * QR de acceso. Si [undoSecondsLeft] no es nulo, el acceso es válido y se registrará solo al
     * llegar a cero; mientras tanto el guardia puede tocar «No permitir» (o «Registrar entrada»
     * para confirmarlo ya).
     */
    data class Access(val card: ScanCardModel, val undoSecondsLeft: Int? = null) : PanelScanResult()

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
 * Bitácora (solo se agregan eventos, nunca se editan):
 * - QR rechazado: se registra al momento.
 * - QR válido: se registra como permitido al terminar la ventana de [UNDO_WINDOW_SEC] segundos,
 *   o antes si el guardia toca «Registrar entrada», escanea otro código o sale del panel.
 *   Si toca «No permitir» dentro de la ventana, se registra como denegado por el guardia.
 * El nonce se consume al escanear, así que el QR ya no puede reutilizarse.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val qr: QrAccessService,
    private val external: ExternalQrInput,
    private val students: StudentRepository,
    private val auth: AuthRepository,
    settings: SettingsRepository
) : ViewModel() {
    companion object {
        /** Segundos para «No permitir» antes de que la entrada se registre sola. */
        const val UNDO_WINDOW_SEC = 5
        private const val TICK_MS = 1_000L
    }

    private val _ui = MutableStateFlow(ScanUi())
    val ui: StateFlow<ScanUi> = _ui.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Tono al escanear (vibración siempre). */
    val soundEnabled: Boolean = settings.isScanSoundEnabled()

    private val debouncer = ScanDebouncer()
    private var externalJob: Job? = null
    private var undoJob: Job? = null

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
        if (!debouncer.accept(raw, now) || _ui.value.busy) return
        // Con un resultado en pantalla solo se acepta otro código si es una entrada en espera:
        // el siguiente alumno de la fila confirma la anterior.
        if (_ui.value.result != null && pendingAccess == null) return
        undoJob?.cancel()
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            commitPendingAccess(resetUi = false)
            val result = try {
                when (val o = qr.scan(raw, now)) {
                    is ScanOutcome.Access -> {
                        if (o.allowed) {
                            pendingAccess = o
                            PanelScanResult.Access(o.toCard(), undoSecondsLeft = UNDO_WINDOW_SEC)
                        } else {
                            qr.log(o, guard, AccessResult.DENIED)
                            PanelScanResult.Access(o.toCard())
                        }
                    }
                    is ScanOutcome.Registration -> registrationResult(o.check)
                    is ScanOutcome.Invalid -> {
                        qr.logRejectedCode(guard, DenialReason.INVALID_QR)
                        PanelScanResult.Access(ScanCardModel.denied(DenialReason.INVALID_QR, now))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ScanViewModel", "Fallo al procesar el código", e)
                PanelScanResult.Access(ScanCardModel.denied(DenialReason.INVALID_QR, now))
            }
            _ui.update { it.copy(busy = false, result = result) }
            if (pendingAccess != null) startUndoWindow()
        }
    }

    /** Cuenta regresiva de la ventana para deshacer; al terminar, la entrada se registra sola. */
    private fun startUndoWindow() {
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            for (left in UNDO_WINDOW_SEC downTo 1) {
                _ui.update { s ->
                    val r = s.result as? PanelScanResult.Access ?: return@update s
                    s.copy(result = r.copy(undoSecondsLeft = left))
                }
                delay(TICK_MS)
            }
            commitPendingAccess(resetUi = true)
        }
    }

    /** Registra como permitida la entrada en espera (si hay). */
    private suspend fun commitPendingAccess(resetUi: Boolean) {
        val o = pendingAccess ?: return
        pendingAccess = null
        qr.log(o, guard, AccessResult.ALLOWED)
        _messages.tryEmit("Entrada registrada: ${o.student?.nombre ?: o.token.matricula}")
        if (resetUi) _ui.update { ScanUi() }
    }

    private fun registrationResult(c: RegistrationCheck): PanelScanResult.Registration = when (c) {
        is RegistrationCheck.Ok -> {
            pendingRegistration = c.payload
            PanelScanResult.Registration(c.payload, PanelScanResult.Registration.Kind.READY, c.previousStatus, c.replacesKey)
        }
        is RegistrationCheck.Blocked -> PanelScanResult.Registration(c.payload, PanelScanResult.Registration.Kind.BLOCKED, c.status)
        is RegistrationCheck.Invalid -> PanelScanResult.Registration(c.payload, PanelScanResult.Registration.Kind.INVALID)
    }

    /** El guardia confirma ya la entrada, sin esperar la cuenta regresiva. */
    fun registerEntry() {
        if (pendingAccess == null) return
        undoJob?.cancel()
        _ui.update { it.copy(working = true) }
        viewModelScope.launch { commitPendingAccess(resetUi = true) }
    }

    /** El guardia decide no permitir el acceso pese a que el QR es válido (dentro de la ventana). */
    fun denyEntry() {
        val o = pendingAccess ?: return
        pendingAccess = null
        undoJob?.cancel()
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

    /** Cierra el resultado y vuelve a escanear (una entrada en espera se confirma). */
    fun dismiss() {
        if (pendingAccess != null) return registerEntry()
        pendingRegistration = null
        _ui.update { ScanUi() }
    }

    override fun onCleared() {
        // Salió del panel durante la ventana: el acceso era válido y nadie lo objetó.
        pendingAccess?.let { qr.logDetached(it, guard, AccessResult.ALLOWED, null) }
        pendingAccess = null
    }
}
