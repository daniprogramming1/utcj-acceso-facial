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
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val qr: QrTokenManager
) : ViewModel() {

    data class VerifyOutcome(val allowed: Boolean, val nombre: String, val matricula: String)
    data class Ui(
        val guidance: String = "Colócate frente a la cámara",
        val busy: Boolean = false,
        val modeQr: Boolean = false,
        val result: VerifyOutcome? = null,
        val livenessState: LivenessChecker.LivenessState = LivenessChecker.LivenessState()
    )

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
        _ui.value = _ui.value.copy(modeQr = true, guidance = "Muestra el código QR dinámico")
    }

    fun clearResult() {
        _ui.value = _ui.value.copy(result = null, busy = false, modeQr = false)
    }

    suspend fun onFrame(bitmap: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastVerifyMs < 700 || _ui.value.busy) return
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
                _ui.value = _ui.value.copy(guidance = q.guidanceEs, busy = false)
                return
            }
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
            _ui.value = _ui.value.copy(guidance = "Error: ${e.message}", busy = false)
        }
    }

    private suspend fun finishFace(match: FaceMatcher.MatchResult, duration: Long) {
        if (!match.matched || match.matricula == null) {
            accessLog.log("—", "Desconocido", AccessResult.DENIED, AccessMethod.FACE, similarity = match.bestSimilarity, durationMs = duration)
            _ui.value = _ui.value.copy(
                result = VerifyOutcome(false, "No reconocido", "—"),
                busy = false,
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
            !withinHours -> "Fuera de horario"
            else -> null
        }
        accessLog.log(
            match.matricula, nombre, result, AccessMethod.FACE,
            similarity = match.bestSimilarity, durationMs = duration, reason = reason
        )
        _ui.value = _ui.value.copy(
            result = VerifyOutcome(allowed, nombre, match.matricula),
            busy = false
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
                    _ui.value = _ui.value.copy(
                        result = VerifyOutcome(allowed, nombre, v.matricula),
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
                result = VerifyOutcome(true, "Huella verificada", "BIOMETRIC")
            )
        } else {
            _ui.value = _ui.value.copy(guidance = "Huella no verificada")
        }
    }
}
