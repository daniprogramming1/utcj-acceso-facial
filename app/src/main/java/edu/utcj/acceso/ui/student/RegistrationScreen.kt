package edu.utcj.acceso.ui.student

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.biometric.FaceEmbeddingEngine
import edu.utcj.acceso.data.biometric.FaceQualityChecker
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.CameraPreview
import edu.utcj.acceso.ui.components.PrimaryBigButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@Composable
fun RegistrationScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: RegistrationViewModel = hiltViewModel()
) {
    val state by vm.ui.collectAsState()
    var matricula by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var carrera by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        AccesoHeader(
            title = "Registro facial",
            subtitle = "Captura ${state.samples.size}/${state.maxSamples} muestras",
            onBack = onBack
        )
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(matricula, { matricula = it }, label = { Text("Matrícula") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(carrera, { carrera = it }, label = { Text("Carrera") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(state.guidance)
            LinearProgressIndicator(
                progress = { state.samples.size.toFloat() / state.maxSamples },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                CameraPreview { bmp ->
                    if (!state.capturing && state.samples.size < state.maxSamples) {
                        scope.launch { vm.onFrame(bmp) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton(
                text = if (state.samples.size >= state.minSamples) "Guardar registro" else "Capturando…",
                enabled = state.samples.size >= state.minSamples && matricula.isNotBlank() && nombre.isNotBlank() && !state.saving,
                onClick = {
                    scope.launch {
                        val ok = vm.save(matricula.trim(), nombre.trim(), carrera.trim())
                        if (ok) onDone()
                    }
                }
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val students: StudentRepository,
    private val settings: SettingsRepository,
    private val engine: FaceEmbeddingEngine,
    private val quality: FaceQualityChecker
) : ViewModel() {
    data class Ui(
        val samples: List<FloatArray> = emptyList(),
        val guidance: String = "Mira de frente a la cámara",
        val capturing: Boolean = false,
        val saving: Boolean = false,
        val error: String? = null,
        val minSamples: Int = 3,
        val maxSamples: Int = 5
    )

    private val _ui = MutableStateFlow(
        Ui(minSamples = settings.getMinSamples(), maxSamples = settings.getMaxSamples())
    )
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    private var lastCaptureMs = 0L

    suspend fun onFrame(bitmap: Bitmap) {
        val now = System.currentTimeMillis()
        if (now - lastCaptureMs < 900) return
        val cur = _ui.value
        if (cur.samples.size >= cur.maxSamples || cur.capturing) return
        _ui.value = cur.copy(capturing = true)
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            val q = quality.evaluate(bitmap, faces)
            if (!q.ok) {
                _ui.value = _ui.value.copy(guidance = q.guidanceEs, capturing = false)
                return
            }
            val emb = engine.extractEmbedding(bitmap, faces.first())
            lastCaptureMs = now
            val next = _ui.value.samples + emb
            _ui.value = _ui.value.copy(
                samples = next,
                guidance = "Muestra ${next.size} capturada. ${q.guidanceEs}",
                capturing = false
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(guidance = "Error de cámara: ${e.message}", capturing = false)
        }
    }

    suspend fun save(matricula: String, nombre: String, carrera: String): Boolean {
        _ui.value = _ui.value.copy(saving = true, error = null)
        return try {
            students.registerWithConsent(matricula, nombre, carrera, _ui.value.samples)
            true
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(saving = false, error = e.message)
            false
        }
    }
}
