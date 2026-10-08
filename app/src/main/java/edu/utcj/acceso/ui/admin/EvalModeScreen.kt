package edu.utcj.acceso.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.local.EvalResultDao
import edu.utcj.acceso.data.local.EvalResultEntity
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppListItem
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SectionHeader
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Modo oculto de evaluación (7 toques en la versión): registra escenarios de prueba manual. */
@HiltViewModel
class EvalModeViewModel @Inject constructor(
    private val dao: EvalResultDao,
    private val auth: AuthRepository
) : ViewModel() {
    val results = dao.observeAll()
    suspend fun record(scenario: String, passed: Boolean, notes: String, sim: Float?) {
        dao.insert(
            EvalResultEntity(
                scenario = scenario, passed = passed, notes = notes, similarity = sim, recordedBy = auth.currentGuardName()
            )
        )
    }
}

private val scenarios = listOf("Lentes", "Cubrebocas", "Poca luz", "Contraluz", "Ángulo", "Gorra", "Gemelos", "Foto impresa")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvalModeScreen(onBack: () -> Unit, vm: EvalModeViewModel = hiltViewModel()) {
    val results by vm.results.collectAsState(initial = emptyList())
    var scenario by remember { mutableStateOf(scenarios.first()) }
    var notes by remember { mutableStateOf("") }
    var passed by remember { mutableStateOf(true) }
    var similarity by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val pass = results.count { it.passed }
    Scaffold(topBar = { AccesoTopBar(title = "Modo evaluación", subtitle = "Pruebas manuales del motor", onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.screenCompact, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                AlertBanner("Herramienta interna", Tone.Info, Icons.Rounded.Science,
                    message = "Documenta escenarios difíciles para calibrar el umbral. No afecta la bitácora.")
                Spacer(Modifier.height(Spacing.lg))
                Text("Escenario", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    scenarios.forEach { s -> FilterChip(scenario == s, { scenario = s }, label = { Text(s) }) }
                }
                Spacer(Modifier.height(Spacing.sm))
                AppTextField(scenario, { scenario = it }, "Escenario (personalizado)")
                Spacer(Modifier.height(Spacing.sm))
                AppTextField(similarity, { similarity = it }, "Similitud observada (opcional)", keyboardType = KeyboardType.Decimal)
                Spacer(Modifier.height(Spacing.sm))
                AppTextField(notes, { notes = it }, "Notas", singleLine = false, minLines = 2)
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(passed, { passed = true }, label = { Text("Pasó") })
                    FilterChip(!passed, { passed = false }, label = { Text("Falló") })
                }
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton("Registrar resultado", onClick = {
                    scope.launch {
                        vm.record(scenario, passed, notes, similarity.replace(',', '.').toFloatOrNull())
                        notes = ""; similarity = ""
                    }
                }, modifier = Modifier.fillMaxWidth(), enabled = scenario.isNotBlank())
                Spacer(Modifier.height(Spacing.xl))
                SectionHeader("Resultados", subtitle = if (results.isEmpty()) "Sin registros" else "$pass de ${results.size} pasaron")
            }
            items(results, key = { it.id }) { r ->
                AppListItem(
                    title = r.scenario,
                    subtitle = TimeUtil.formatDateTime(r.recordedAtMs) + (r.similarity?.let { " · sim %.2f".format(it) } ?: ""),
                    supporting = r.notes.ifBlank { null },
                    trailing = { StatusPill(if (r.passed) "Pasó" else "Falló", if (r.passed) Tone.Success else Tone.Danger) }
                )
            }
        }
    }
}
