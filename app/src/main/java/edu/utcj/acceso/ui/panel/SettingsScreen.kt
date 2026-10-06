package edu.utcj.acceso.ui.panel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    var threshold by remember { mutableFloatStateOf(vm.threshold()) }
    var liveness by remember { mutableStateOf(vm.liveness()) }
    var startH by remember { mutableIntStateOf(vm.hoursStart()) }
    var endH by remember { mutableIntStateOf(vm.hoursEnd()) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openInputStream(uri)?.use { vm.importCsv(it) }
                msg = "CSV importado"
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Configuración", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            Text("Umbral facial (coseno). Predeterminado: 0.72")
            OutlinedTextField(
                value = threshold.toString(),
                onValueChange = { it.toFloatOrNull()?.let { v -> threshold = v } },
                label = { Text("Umbral") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Liveness (parpadeo / giro)")
            Switch(checked = liveness, onCheckedChange = { liveness = it })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(startH.toString(), { it.toIntOrNull()?.let { v -> startH = v } }, label = { Text("Hora inicio (0-23)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(endH.toString(), { it.toIntOrNull()?.let { v -> endH = v } }, label = { Text("Hora fin (0-23)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            PrimaryBigButton("Guardar", onClick = {
                vm.save(threshold, liveness, startH, endH)
                msg = "Guardado"
            })
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Importar estatus CSV", onClick = { picker.launch(arrayOf("text/*", "text/csv")) })
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Cargar CSV de assets", onClick = {
                scope.launch { vm.loadAssets(); msg = "Assets cargados" }
            })
            msg?.let { Text(it) }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class SettingsViewModel @javax.inject.Inject constructor(
    private val settings: edu.utcj.acceso.data.repository.SettingsRepository,
    private val students: edu.utcj.acceso.data.repository.StudentRepository,
    private val statusRepo: edu.utcj.acceso.data.repository.StudentStatusRepository
) : androidx.lifecycle.ViewModel() {
    fun threshold() = settings.getFaceThreshold()
    fun liveness() = settings.isLivenessEnabled()
    fun hoursStart() = settings.getHoursStart()
    fun hoursEnd() = settings.getHoursEnd()
    fun save(t: Float, live: Boolean, s: Int, e: Int) {
        settings.setFaceThreshold(t)
        settings.setLivenessEnabled(live)
        settings.setHours(s, e)
    }
    suspend fun importCsv(input: java.io.InputStream) = students.importStatusFromStream(input)
    suspend fun loadAssets() = statusRepo.refreshFromInstitutionalSource()
}
