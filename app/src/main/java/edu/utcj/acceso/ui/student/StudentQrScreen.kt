package edu.utcj.acceso.ui.student

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.biometric.QrTokenManager
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * QR dinámico firmado con HMAC-SHA256, se regenera cada 30 s.
 * Limitación: el secreto HMAC es local al dispositivo (SettingsRepository), por lo que
 * el QR se valida en el mismo dispositivo/kiosco o en dispositivos que compartan el secreto.
 */
@Composable
fun StudentQrScreen(onBack: () -> Unit, vm: StudentQrViewModel = hiltViewModel()) {
    var matricula by remember { mutableStateOf("") }
    var activeMat by remember { mutableStateOf<String?>(null) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeMat) {
        val mat = activeMat ?: return@LaunchedEffect
        while (true) {
            qr = vm.renderQr(vm.issue(mat))
            for (s in QrTokenManager.VALIDITY_SECONDS.toInt() downTo 1) {
                secondsLeft = s
                delay(1_000)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Mi QR dinámico", subtitle = "Válido 30 segundos", onBack = onBack)
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeMat == null) {
                OutlinedTextField(
                    matricula, { matricula = it },
                    label = { Text("Matrícula") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                PrimaryBigButton("Generar QR", enabled = matricula.isNotBlank(), onClick = {
                    scope.launch {
                        val msg = vm.eligibilityError(matricula.trim())
                        if (msg == null) { error = null; activeMat = matricula.trim() } else error = msg
                    }
                })
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            } else {
                qr?.let {
                    Image(it.asImageBitmap(), contentDescription = "Código QR de acceso", modifier = Modifier.size(280.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Se renueva en $secondsLeft s", style = MaterialTheme.typography.titleLarge)
                Text("Muéstralo a la cámara del kiosco.")
                Spacer(Modifier.height(16.dp))
                PrimaryBigButton("Terminar", onClick = { activeMat = null; qr = null })
            }
        }
    }
}

@HiltViewModel
class StudentQrViewModel @Inject constructor(
    private val qrTokens: QrTokenManager,
    private val students: StudentRepository
) : ViewModel() {

    suspend fun eligibilityError(matricula: String): String? {
        val s = students.get(matricula) ?: return "Matrícula no registrada"
        return when (s.status) {
            StudentStatus.APPROVED, StudentStatus.ACTIVO -> null
            StudentStatus.PENDING -> "Tu registro aún está pendiente de aprobación"
            StudentStatus.BAJA -> "Estatus BAJA: acceso no permitido"
            StudentStatus.SUSPENDIDO -> "Estatus SUSPENDIDO: acceso no permitido"
            StudentStatus.REJECTED -> "Registro rechazado; acude con seguridad"
        }
    }

    fun issue(matricula: String): String = qrTokens.issue(matricula)

    fun renderQr(content: String, sizePx: Int = 640): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) for (x in 0 until sizePx) {
            pixels[y * sizePx + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }
}
