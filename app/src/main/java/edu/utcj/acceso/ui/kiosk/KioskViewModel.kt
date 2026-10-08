package edu.utcj.acceso.ui.kiosk

import android.app.Activity
import android.graphics.Bitmap
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.biometric.FaceEmbeddingEngine
import edu.utcj.acceso.data.biometric.FaceMatcher
import edu.utcj.acceso.data.biometric.FaceQualityChecker
import edu.utcj.acceso.data.biometric.LivenessChecker
import edu.utcj.acceso.data.biometric.QrTokenManager
import edu.utcj.acceso.data.biometric.QrVerifyResult
import edu.utcj.acceso.data.repository.AccessLogRepository
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.IncidentRepository
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.data.repository.SyncRepository
import edu.utcj.acceso.data.repository.SyncUiState
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.components.FaceGuideStatus
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class KioskViewModel @Inject constructor(
    private val students: StudentRepository,
    private val accessLog: AccessLogRepository,
    private val settings: SettingsRepository,
    private val auth: AuthRepository,
    private val engine: FaceEmbeddingEngine,
    private val matcher: FaceMatcher,
    private val quality: FaceQualityChecker,
    private val liveness: LivenessChecker,
    private val qr: QrTokenManager,
    private val incidents: IncidentRepository,
    syncRepository: SyncRepository
) : ViewModel() {

    /** Estado de conexión para la píldora del kiosco. */
    val sync: StateFlow<SyncUiState?> = syncRepository.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)


    data class VerifyOutcome(
        val allowed: Boolean,
        val nombre: String,
        val matricula: String,
        val reason: String? = null,
        val method: AccessMethod = AccessMethod.FACE,
        val timeMs: Long = System.currentTimeMillis()
    )
    data class Ui(
        val guidance: String = IDLE_GUIDANCE,
        val busy: Boolean = false,
        val modeQr: Boolean = false,
        val result: VerifyOutcome? = null,
        val faceStatus: FaceGuideStatus = FaceGuideStatus.Searching,
        val assistanceRequested: Boolean = false,
        val livenessState: LivenessChecker.LivenessState = LivenessChecker.LivenessState()
    ) {
        /** Nadie frente a la cámara: se muestra el estado de espera «Acércate a la cámara». */
        val idle: Boolean get() = !modeQr && result == null &&
            (guidance == IDLE_GUIDANCE || guidance.startsWith("No se detect"))
    }

    companion object {
        const val IDLE_GUIDANCE = "Acércate a la cámara"
    }

    /** Ajustes del kiosco leídos al abrirlo. */
    val idleMs: Long = settings.getKioskIdleMs()
    val soundEnabled: Boolean = settings.isKioskSoundEnabled()
    val orientation: SettingsRepository.KioskOrientation = settings.getKioskOrientation()

    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
    )
    private val barcodeScanner = BarcodeScanning.getClient()
    private var lastVerifyMs = 0L
    private var gallery: Map<String, List<FloatArray>> = emptyMap()

    init {
        viewModelScope.launch { gallery = students.loadGalleryEmbeddings() }
        if (settings.isLivenessEnabled()) {
            _ui.value = _ui.value.copy(
                livenessState = LivenessChecker.LivenessState(
                    challenge = liveness.nextChallenge(),
                    guidanceEs = liveness.guidance(liveness.nextChallenge())
                ),
                guidance = liveness.guidance(liveness.nextChallenge())
            )
        }
    }

    fun setModeQr() {
        _ui.value = _ui.value.copy(modeQr = true, result = null, busy = false, guidance = "Muestra tu QR dinámico a la cámara")
    }

    fun setModeFace() {
        _ui.value = _ui.value.copy(modeQr = false, guidance = IDLE_GUIDANCE, faceStatus = FaceGuideStatus.Searching)
    }

    fun clearResult() {
        lastVerifyMs = System.currentTimeMillis()
        _ui.value = _ui.value.copy(
            result = null, busy = false, modeQr = false, assistanceRequested = false,
            guidance = IDLE_GUIDANCE, faceStatus = FaceGuideStatus.Searching
        )
    }

    /**
     * «Llamar al guardia» desde el resultado denegado: deja constancia como incidencia
     * para que aparezca en el panel (Incidencias) del personal de seguridad.
     */
    fun requestAssistance() {
        val r = _ui.value.result
        viewModelScope.launch {
            incidents.report(
                category = IncidentCategory.ACCESO_NO_AUTORIZADO,
                description = "Solicitud de asistencia desde el kiosco" +
                    (r?.let { " · ${it.nombre} (${it.matricula}) · ${it.reason ?: "Acceso denegado"}" } ?: ""),
                guard = "Kiosco",
                matricula = r?.matricula?.takeIf { it != "—" && it != "BIOMETRIC" }
            )
        }
        _ui.value = _ui.value.copy(assistanceRequested = true)
    }

    suspend fun onFrame(bitmap: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastVerifyMs < 700 || _ui.value.busy || _ui.value.result != null) return
        if (_ui.value.modeQr) {
            processQr(bitmap)
            return
        }
        _ui.value = _ui.value.copy(busy = true)
        val started = now
        try {
            val faces = faceDetector.process(InputImage.fromBitmap(bitmap, 0)).await()
            val q = quality.evaluate(bitmap, faces)
            if (!q.ok) {
                _ui.value = _ui.value.copy(
                    guidance = if (faces.isEmpty()) IDLE_GUIDANCE else q.guidanceEs,
                    busy = false,
                    faceStatus = if (faces.isEmpty()) FaceGuideStatus.Searching else FaceGuideStatus.Adjust
                )
                return
            }
            _ui.value = _ui.value.copy(faceStatus = FaceGuideStatus.Good, guidance = "Verificando…")
            val face = faces.first()
            if (settings.isLivenessEnabled() && !_ui.value.livenessState.completed) {
                val ls = liveness.update(_ui.value.livenessState, face)
                _ui.value = _ui.value.copy(livenessState = ls, guidance = ls.guidanceEs, busy = false)
                if (!ls.completed) return
            }
            if (gallery.isEmpty()) gallery = students.loadGalleryEmbeddings()
            val emb = engine.extractEmbedding(bitmap, face)
            val match = matcher.bestMatch(emb, gallery, settings.getFaceThreshold())
            lastVerifyMs = System.currentTimeMillis()
            finishFace(match, System.currentTimeMillis() - started)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(guidance = "Error: ${e.message}", busy = false, faceStatus = FaceGuideStatus.Error)
        }
    }

    private suspend fun finishFace(match: FaceMatcher.MatchResult, duration: Long) {
        if (!match.matched || match.matricula == null) {
            accessLog.log("—", "Desconocido", AccessResult.DENIED, AccessMethod.FACE, similarity = match.bestSimilarity, durationMs = duration)
            _ui.value = _ui.value.copy(
                result = VerifyOutcome(false, "No reconocido", "—", reason = "Rostro no registrado o no coincide"),
                busy = false,
                faceStatus = FaceGuideStatus.Error,
                guidance = "No se encontró coincidencia"
            )
            return
        }
        val student = students.get(match.matricula)
        val withinHours = TimeUtil.isWithinHours(
            System.currentTimeMillis(), settings.getHoursStart(), settings.getHoursEnd()
        )
        val statusOk = student != null &&
            student.status != StudentStatus.BAJA &&
            student.status != StudentStatus.SUSPENDIDO &&
            student.status != StudentStatus.REJECTED &&
            student.status != StudentStatus.PENDING &&
            (student.status == StudentStatus.APPROVED || student.status == StudentStatus.ACTIVO)

        val allowed = statusOk && withinHours
        val nombre = student?.nombre ?: match.matricula
        val result = if (allowed) AccessResult.ALLOWED else AccessResult.DENIED
        val reason = when {
            student == null -> "Sin registro"
            student.status == StudentStatus.BAJA -> "Estatus BAJA"
            student.status == StudentStatus.SUSPENDIDO -> "Estatus SUSPENDIDO"
            student.status == StudentStatus.PENDING -> "Pendiente de aprobación"
            student.status == StudentStatus.REJECTED -> "Registro rechazado"
            !withinHours -> "Fuera de horario"
            else -> null
        }
        accessLog.log(
            match.matricula, nombre, result, AccessMethod.FACE,
            similarity = match.bestSimilarity, durationMs = duration, reason = reason
        )
        _ui.value = _ui.value.copy(
            result = VerifyOutcome(allowed, nombre, match.matricula, reason = reason, method = AccessMethod.FACE),
            busy = false,
            faceStatus = if (allowed) FaceGuideStatus.Done else FaceGuideStatus.Error
        )
    }

    private suspend fun processQr(bitmap: Bitmap) {
        _ui.value = _ui.value.copy(busy = true)
        try {
            val barcodes = barcodeScanner.process(InputImage.fromBitmap(bitmap, 0)).await()
            val raw = barcodes.firstOrNull()?.rawValue
            if (raw == null) {
                _ui.value = _ui.value.copy(busy = false, guidance = "Buscando QR…")
                return
            }
            when (val v = qr.verify(raw)) {
                is QrVerifyResult.Valid -> {
                    val student = students.get(v.matricula)
                    val allowed = student != null &&
                        (student.status == StudentStatus.APPROVED || student.status == StudentStatus.ACTIVO)
                    val nombre = student?.nombre ?: v.matricula
                    accessLog.log(
                        v.matricula, nombre,
                        if (allowed) AccessResult.QR else AccessResult.DENIED,
                        AccessMethod.QR
                    )
                    lastVerifyMs = System.currentTimeMillis()
                    val qrReason = when {
                        student == null -> "Matrícula sin registro"
                        allowed -> null
                        else -> "Estatus ${student.status.name}"
                    }
                    _ui.value = _ui.value.copy(
                        result = VerifyOutcome(allowed, nombre, v.matricula, reason = qrReason, method = AccessMethod.QR),
                        busy = false,
                        modeQr = false
                    )
                }
                is QrVerifyResult.Invalid -> {
                    _ui.value = _ui.value.copy(busy = false, guidance = v.reason)
                }
            }
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(busy = false, guidance = e.message ?: "Error QR")
        }
    }

    suspend fun verifyFingerprint(activity: Activity) {
        val can = BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            _ui.value = _ui.value.copy(guidance = "Huella no disponible en este dispositivo")
            return
        }
        val fragmentActivity = activity as? FragmentActivity
        if (fragmentActivity == null) {
            _ui.value = _ui.value.copy(guidance = "Biometría requiere FragmentActivity")
            return
        }
        val ok = suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        cont.resume(true)
                    }
                    override fun onAuthenticationError(code: Int, err: CharSequence) {
                        cont.resume(false)
                    }
                    override fun onAuthenticationFailed() {
                        // keep waiting
                    }
                }
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verificación de huella")
                    .setSubtitle("Acceso UTCJ")
                    .setNegativeButtonText("Cancelar")
                    .build()
            )
        }
        // Device biometric confirms presence of enrolled fingerprint; for school demo
        // we still need a selected student — log as fallback requiring guard confirmation path.
        if (ok) {
            accessLog.log(
                "BIOMETRIC", "Huella del dispositivo",
                AccessResult.ALLOWED, AccessMethod.FINGERPRINT,
                guard = auth.currentGuardName(),
                reason = "Fallback biométrico del dispositivo"
            )
            _ui.value = _ui.value.copy(
                result = VerifyOutcome(true, "Huella verificada", "BIOMETRIC", method = AccessMethod.FINGERPRINT)
            )
        } else {
            _ui.value = _ui.value.copy(guidance = "Huella no verificada")
        }
    }
}
