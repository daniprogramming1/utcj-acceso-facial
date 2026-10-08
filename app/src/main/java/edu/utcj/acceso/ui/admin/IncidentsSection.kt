package edu.utcj.acceso.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.IncidentRepository
import edu.utcj.acceso.domain.model.Incident
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.EmptyState
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.SuccessIllustration
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.components.tone
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repo: IncidentRepository,
    private val auth: AuthRepository
) : ViewModel() {
    val incidents: StateFlow<List<Incident>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun report(c: IncidentCategory, d: String, matricula: String?) = viewModelScope.launch {
        repo.report(c, d, auth.currentGuardName(), photoUri = null, matricula = matricula)
    }
}

@Composable
fun IncidentsSection(vm: IncidentsViewModel = hiltViewModel()) {
    val list by vm.incidents.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    IncidentsContent(list, snackbar) { c, d, m ->
        vm.report(c, d, m)
        scope.launch { snackbar.showSnackbar("Incidencia registrada") }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IncidentsContent(
    incidents: List<Incident>,
    snackbar: SnackbarHostState? = null,
    onReport: (IncidentCategory, String, String?) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    SectionScaffold(
        title = "Incidencias",
        subtitle = "${incidents.size} reportes",
        snackbarHostState = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Reportar") }
            )
        }
    ) { padding ->
        if (incidents.isEmpty()) {
            EmptyState(
                "Sin incidencias",
                "Aquí verás los reportes de seguridad y las solicitudes de ayuda hechas desde el kiosco.",
                illustration = { SuccessIllustration(Modifier.height(140.dp)) },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = Spacing.screenCompact, end = Spacing.screenCompact, top = Spacing.sm, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(incidents, key = { it.id }) { i ->
                    AppCard(Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.ReportProblem, i.category.tone())
                            Spacer(Modifier.padding(start = Spacing.md))
                            Column(Modifier.weight(1f)) {
                                StatusPill(i.category.labelEs(), i.category.tone())
                                Spacer(Modifier.height(2.dp))
                                Text(TimeUtil.formatDateTime(i.datetimeMs), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        Text(i.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Badge, contentDescription = null, modifier = Modifier.height(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.padding(start = Spacing.xs))
                            Text(
                                "Reportó: ${i.reportedByGuard}${i.relatedMatricula?.let { " · Matrícula $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        var category by remember { mutableStateOf(IncidentCategory.SEGURIDAD) }
        var description by remember { mutableStateOf("") }
        var matricula by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showForm = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding().imePadding().padding(bottom = Spacing.xl)) {
                Text("Reportar incidencia", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.lg))
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    IncidentCategory.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.labelEs()) })
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                AppTextField(description, { description = it }, "Descripción", singleLine = false, minLines = 3)
                Spacer(Modifier.height(Spacing.sm))
                AppTextField(matricula, { matricula = it }, "Matrícula relacionada (opcional)", leadingIcon = Icons.Rounded.Badge)
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton(
                    "Guardar reporte",
                    onClick = {
                        onReport(category, description.trim(), matricula.trim().uppercase().ifBlank { null })
                        showForm = false
                    },
                    enabled = description.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
