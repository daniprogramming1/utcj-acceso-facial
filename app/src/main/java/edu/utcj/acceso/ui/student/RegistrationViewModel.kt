package edu.utcj.acceso.ui.student

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.biometric.FaceEmbeddingEngine
import edu.utcj.acceso.data.biometric.FaceQualityChecker
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.validation.RegistrationValidator
import edu.utcj.acceso.ui.components.FaceGuideStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class RegStep(val label: String) {
    DATA("Datos"),
    CONSENT("Consentimiento"),
    CAPTURE("Captura facial"),
    DONE("Listo")
}

data class RegistrationUi(
    val step: RegStep = RegStep.DATA,
    val matricula: String = "",
    val nombre: String = "",
    val carrera: String = "",
    val showErrors: Boolean = false,
    val consentAccepted: Boolean = false,
    /** Estatus previo si la matrícula ya tenía muestras faciales registradas. */
    val existingStatus: StudentStatus? = null,
    val samples: List<FloatArray> = emptyList(),
    val guidance: String = "Mira de frente a la cámara",
    val faceStatus: FaceGuideStatus = FaceGuideStatus.Searching,
    val capturing: Boolean = false,
    val saving: Boolean = false,
    val checking: Boolean = false,
    val error: String? = null,
    val minSamples: Int = 3,
    val maxSamples: Int = 5,
    val savedStatus: StudentStatus? = null
) {
    val matriculaError: String? get() = if (showErrors) RegistrationValidator.matriculaError(matricula) else null
    val nombreError: String? get() = if (showErrors) RegistrationValidator.nombreError(nombre) else null
    val canSave: Boolean get() = samples.size >= minSamples && !saving
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val students: StudentRepository,
    private val settings: SettingsRepository,
    private val engine: FaceEmbeddingEngine,
    private val quality: FaceQualityChecker
) : ViewModel() {

    private val _ui = MutableStateFlow(
        RegistrationUi(minSamples = settings.getMinSamples(), maxSamples = settings.getMaxSamples())
    )
    val ui: StateFlow<RegistrationUi> = _ui.asStateFlow()

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    private var lastCaptureMs = 0L

    fun onMatricula(v: String) = _ui.update { it.copy(matricula = v.take(RegistrationValidator.MATRICULA_MAX + 5)) }
    fun onNombre(v: String) = _ui.update { it.copy(nombre = v.take(80)) }
    fun onCarrera(v: String) = _ui.update { it.copy(carrera = v.take(80)) }
    fun onConsent(v: Boolean) = _ui.update { it.copy(consentAccepted = v) }

    /** Avanza al siguiente paso si el actual es válido. */
    fun next() {
        val s = _ui.value
        when (s.step) {
            RegStep.DATA -> {
                if (!RegistrationValidator.isValid(s.matricula, s.nombre)) {
                    _ui.update { it.copy(showErrors = true) }
                    return
                }
                val mat = RegistrationValidator.normalizeMatricula(s.matricula)
                _ui.update { it.copy(checking = true, matricula = mat) }
                viewModelScope.launch {
                    val existing = students.get(mat)
                    val hasSamples = existing != null && students.sampleCount(mat) > 0
                    _ui.update {
                        it.copy(
                            checking = false,
                            existingStatus = if (hasSamples) existing?.status
                            else existing?.status?.takeIf { st -> st == StudentStatus.BAJA || st == StudentStatus.SUSPENDIDO },
                            step = RegStep.CONSENT
                        )
                    }
                }
            }
            RegStep.CONSENT -> if (s.consentAccepted) _ui.update { it.copy(step = RegStep.CAPTURE) }
            RegStep.CAPTURE -> save()
            RegStep.DONE -> Unit
        }
    }

    /** @return false si ya está en el primer paso (la pantalla debe cerrar). */
    fun back(): Boolean {
        val s = _ui.value
        return when (s.step) {
            RegStep.DATA, RegStep.DONE -> false
            RegStep.CONSENT -> { _ui.update { it.copy(step = RegStep.DATA) }; true }
            RegStep.CAPTURE -> { _ui.update { it.copy(step = RegStep.CONSENT) }; true }
        }
    }

    fun resetCapture() {
        lastCaptureMs = 0L
        _ui.update { it.copy(samples = emptyList(), guidance = "Mira de frente a la cámara", faceStatus = FaceGuideStatus.Searching) }
    }

    suspend fun onFrame(bitmap: Bitmap) {
        val now = System.currentTimeMillis()
        val cur = _ui.value
        if (cur.step != RegStep.CAPTURE || now - lastCaptureMs < 900) return
        if (cur.samples.size >= cur.maxSamples || cur.capturing) return
        _ui.update { it.copy(capturing = true) }
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            val q = quality.evaluate(bitmap, faces)
            if (!q.ok) {
                _ui.update {
                    it.copy(
                        guidance = q.guidanceEs,
                        capturing = false,
                        faceStatus = if (faces.isEmpty()) FaceGuideStatus.Searching else FaceGuideStatus.Adjust
                    )
                }
                return
            }
            val emb = engine.extractEmbedding(bitmap, faces.first())
            lastCaptureMs = now
            _ui.update {
                val next = it.samples + emb
                val done = next.size >= it.maxSamples
                it.copy(
                    samples = next,
                    guidance = if (done) "¡Listo! Muestras completas" else "Muestra ${next.size} capturada",
                    faceStatus = if (done) FaceGuideStatus.Done else FaceGuideStatus.Good,
                    capturing = false
                )
            }
        } catch (e: Exception) {
            _ui.update { it.copy(guidance = "Error de cámara: ${e.message}", capturing = false, faceStatus = FaceGuideStatus.Error) }
        }
    }

    private fun save() {
        val s = _ui.value
        if (!s.canSave) return
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val saved = students.registerWithConsent(s.matricula, s.nombre.trim(), s.carrera.trim(), s.samples)
                _ui.update { it.copy(saving = false, step = RegStep.DONE, savedStatus = saved.status) }
            } catch (e: Exception) {
                _ui.update { it.copy(saving = false, error = e.message ?: "No se pudo guardar el registro") }
            }
        }
    }

    /** Recuerda la matrícula en este dispositivo para el «Inicio del alumno». */
    fun rememberOnDevice() = settings.setRememberedStudent(_ui.value.matricula)
}
