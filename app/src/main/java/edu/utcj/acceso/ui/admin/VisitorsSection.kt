package edu.utcj.acceso.ui.admin

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.rounded.Topic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.VisitorRepository
import edu.utcj.acceso.domain.model.Visitor
import edu.utcj.acceso.ui.components.AppListItem
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.EmptyState
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VisitorsViewModel @Inject constructor(
    private val repo: VisitorRepository,
    private val auth: AuthRepository
) : ViewModel() {
    val visitors: StateFlow<List<Visitor>?> = repo.observeAll().map<List<Visitor>, List<Visitor>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun register(n: String, m: String, v: String) = viewModelScope.launch { repo.register(n, m, v, auth.currentGuardName()) }
    fun exit(id: Long) = viewModelScope.launch { repo.markExit(id) }
}

@Composable
fun VisitorsSection(vm: VisitorsViewModel = hiltViewModel()) {
    val visitors by vm.visitors.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    VisitorsContent(
        visitors = visitors,
        snackbar = snackbar,
        onRegister = { n, m, v -> vm.register(n, m, v); scope.launch { snackbar.showSnackbar("Visitante registrado") } },
        onExit = { vm.exit(it.id); scope.launch { snackbar.showSnackbar("Salida de ${it.nombre} registrada") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorsContent(
    visitors: List<Visitor>?,
    snackbar: SnackbarHostState? = null,
    onRegister: (String, String, String) -> Unit,
    onExit: (Visitor) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showForm by remember { mutableStateOf(false) }
    val inside = visitors.orEmpty().filter { it.isInside }
    val list = if (tab == 0) inside else visitors.orEmpty()
    SectionScaffold(
        title = "Visitantes",
        subtitle = "${inside.size} dentro del plantel",
        snackbarHostState = snackbar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Registrar visita") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Dentro (${inside.size})") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Historial") })
            }
            if (list.isEmpty()) {
                EmptyState(
                    if (tab == 0) "No hay visitantes dentro" else "Sin visitas registradas",
                    "Registra a cada visitante al llegar y marca su salida al irse.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(top = Spacing.sm, bottom = 96.dp)) {
                    items(list, key = { it.id }) { v ->
                        AppListItem(
                            title = v.nombre,
                            subtitle = "Visita a ${v.visitaA} · ${v.motivo}",
                            supporting = "Entrada ${TimeUtil.formatTime(v.entradaMs)}" +
                                (v.salidaMs?.let { " · Salida ${TimeUtil.formatTime(it)}" } ?: "") +
                                if (tab == 1) " · ${TimeUtil.formatShortDate(v.entradaMs)}" else "",
                            leading = { InitialsAvatar(v.nombre) },
                            trailing = {
                                if (v.isInside) {
                                    TextButton(onClick = { onExit(v) }) {
                                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                        Spacer(Modifier.padding(start = Spacing.xs))
                                        Text("Salida")
                                    }
                                } else StatusPill("Salió", Tone.Neutral)
                            },
                            modifier = Modifier.padding(horizontal = Spacing.sm)
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        var nombre by remember { mutableStateOf("") }
        var motivo by remember { mutableStateOf("") }
        var visitaA by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showForm = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding().imePadding().padding(bottom = Spacing.xl)) {
                Text("Registrar visitante", style = MaterialTheme.typography.titleLarge)
                Text("La hora de entrada se registra automáticamente.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.lg))
                AppTextField(nombre, { nombre = it }, "Nombre completo", leadingIcon = Icons.Rounded.Person)
                Spacer(Modifier.height(Spacing.sm))
                AppTextField(visitaA, { visitaA = it }, "¿A quién visita?", leadingIcon = Icons.Rounded.PersonPin)
                Spacer(Modifier.height(Spacing.sm))
                AppTextField(motivo, { motivo = it }, "Motivo", leadingIcon = Icons.Rounded.Topic, imeAction = ImeAction.Done)
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton(
                    "Registrar entrada",
                    onClick = { onRegister(nombre.trim(), motivo.trim(), visitaA.trim()); showForm = false },
                    enabled = nombre.isNotBlank() && motivo.isNotBlank() && visitaA.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


