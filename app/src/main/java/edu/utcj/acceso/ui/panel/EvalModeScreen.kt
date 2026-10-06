package edu.utcj.acceso.ui.panel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.launch

/**
 * Hidden evaluation mode for manual test scenarios:
 * lenses, mask, low light, etc.
 */
@Composable
fun EvalModeScreen(onBack: () -> Unit, vm: EvalModeViewModel = hiltViewModel()) {
    val results by vm.results.collectAsState(initial = emptyList())
    var scenario by remember { mutableStateOf("lentes") }
    var notes by remember { mutableStateOf("") }
    var passed by remember { mutableStateOf(true) }
    var similarity by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Modo evaluación", subtitle = "Pruebas manuales", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            Text("Escenarios: lentes, cubrebocas, poca luz, ángulo, gemelos…")
            OutlinedTextField(scenario, { scenario = it }, label = { Text("Escenario") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(similarity, { similarity = it }, label = { Text("Similitud (opcional)") }, modifier = Modifier.fillMaxWidth())
            Text("¿Pasó?")
            Switch(checked = passed, onCheckedChange = { passed = it })
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Registrar resultado", onClick = {
                scope.launch {
                    vm.record(scenario, passed, notes, similarity.toFloatOrNull())
                    notes = ""
                }
            })
            LazyColumn {
                items(results, key = { it.id }) { r ->
                    Text(
                        "${TimeUtil.formatDateTime(r.recordedAtMs)} · ${r.scenario} · ${if (r.passed) "OK" else "FAIL"} · ${r.notes}",
                        Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class EvalModeViewModel @javax.inject.Inject constructor(
    private val dao: edu.utcj.acceso.data.local.EvalResultDao,
    private val auth: edu.utcj.acceso.data.repository.AuthRepository
) : androidx.lifecycle.ViewModel() {
    val results = dao.observeAll()
    suspend fun record(scenario: String, passed: Boolean, notes: String, sim: Float?) {
        dao.insert(
            edu.utcj.acceso.data.local.EvalResultEntity(
                scenario = scenario,
                passed = passed,
                notes = notes,
                similarity = sim,
                recordedBy = auth.currentGuardName()
            )
        )
    }
}
