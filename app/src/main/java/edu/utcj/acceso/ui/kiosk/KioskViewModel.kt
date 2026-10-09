package edu.utcj.acceso.ui.kiosk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.qr.ExternalQrInput
import edu.utcj.acceso.data.qr.QrAccessService
import edu.utcj.acceso.data.qr.ScanDebouncer
import edu.utcj.acceso.data.qr.ScanOutcome
import edu.utcj.acceso.data.repository.IncidentRepository
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.SyncRepository
import edu.utcj.acceso.data.repository.SyncUiState
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.ui.scan.ScanCardModel
import edu.utcj.acceso.ui.scan.toCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Kiosco de autoservicio: el alumno muestra su QR de acceso a la cámara y el resultado se
 * registra automáticamente en la bitácora (sin intervención del guardia).
 */
@HiltViewModel
class KioskViewModel @Inject constructor(
    private val qr: QrAccessService,
    private val external: ExternalQrInput,
    private val incidents: IncidentRepository,
    settings: SettingsRepository,
    syncRepository: SyncRepository
) : ViewModel() {

    /** Estado de conexión para la píldora del kiosco. */
    val sync: StateFlow<SyncUiState?> = syncRepository.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    data class Ui(
        val busy: Boolean = false,
        val result: ScanCardModel? = null,
        val assistanceRequested: Boolean = false
    ) {
        val idle: Boolean get() = result == null && !busy
    }

    companion object {
        const val GUARD_LABEL = "Kiosco"
    }

    /** Ajustes del kiosco leídos al abrirlo. */
    val idleMs: Long = settings.getKioskIdleMs()
    val soundEnabled: Boolean = settings.isKioskSoundEnabled()
    val orientation: SettingsRepository.KioskOrientation = settings.getKioskOrientation()

    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()
    private val debouncer = ScanDebouncer()
    private var externalJob: kotlinx.coroutines.Job? = null

    /** La pantalla está visible: acepta también códigos de [ExternalQrInput]. */
    fun setActive(active: Boolean) {
        externalJob?.cancel()
        externalJob = if (active) viewModelScope.launch { external.codes.collect { onCode(it) } } else null
    }

    fun clearResult() = _ui.update { Ui() }

    fun onCode(raw: String) {
        val now = System.currentTimeMillis()
        if (!debouncer.accept(raw, now) || _ui.value.busy || _ui.value.result != null) return
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val card = try {
                when (val o = qr.scan(raw, now)) {
                    is ScanOutcome.Access -> {
                        qr.log(o, GUARD_LABEL, if (o.allowed) AccessResult.ALLOWED else AccessResult.DENIED)
                        o.toCard()
                    }
                    is ScanOutcome.Registration -> {
                        val p = o.check.payload
                        qr.logRejectedCode(GUARD_LABEL, DenialReason.REGISTRATION_QR, p?.matricula, p?.nombre)
                        ScanCardModel.denied(DenialReason.REGISTRATION_QR, now, p?.matricula ?: "—", p?.nombre ?: DenialReason.REGISTRATION_QR.labelEs)
                    }
                    is ScanOutcome.Invalid -> {
                        qr.logRejectedCode(GUARD_LABEL, DenialReason.INVALID_QR)
                        ScanCardModel.denied(DenialReason.INVALID_QR, now)
                    }
                }
            } catch (e: Exception) {
                ScanCardModel.denied(DenialReason.INVALID_QR, now)
            }
            _ui.update { it.copy(busy = false, result = card) }
        }
    }

    fun requestAssistance() {
        val r = _ui.value.result
        viewModelScope.launch {
            incidents.report(
                category = IncidentCategory.ACCESO_NO_AUTORIZADO,
                description = "Solicitud de asistencia desde el kiosco" +
                    (r?.let { " · ${it.nombre} (${it.matricula}) · ${it.reasonTitle ?: "Acceso denegado"}" } ?: ""),
                guard = GUARD_LABEL,
                matricula = r?.matricula?.takeIf { r.known }
            )
        }
        _ui.update { it.copy(assistanceRequested = true) }
    }
}
