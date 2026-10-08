package edu.utcj.acceso.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.components.AppListItem
import edu.utcj.acceso.ui.components.AppSearchField
import edu.utcj.acceso.ui.components.ConfirmDialog
import edu.utcj.acceso.ui.components.DangerButton
import edu.utcj.acceso.ui.components.DetailRow
import edu.utcj.acceso.ui.components.EmptyState
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.SkeletonList
import edu.utcj.acceso.ui.components.StudentStatusChip
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Filtros rápidos de la lista de alumnos. */
enum class StudentFilter(val label: String) {
    ALL("Todos"), APPROVED("Aprobados"), PENDING("Pendientes"), INACTIVE("Baja / suspendidos"), REJECTED("Rechazados");

    fun matches(s: StudentStatus): Boolean = when (this) {
        ALL -> true
        APPROVED -> s.allowsAccess()
        PENDING -> s == StudentStatus.PENDING
        INACTIVE -> s == StudentStatus.BAJA || s == StudentStatus.SUSPENDIDO
        REJECTED -> s == StudentStatus.REJECTED
    }
}

/** Filtro puro (probado en JVM): texto por matrícula/nombre/carrera, sin acentos ni mayúsculas. */
fun filterStudents(list: List<Student>, query: String, filter: StudentFilter): List<Student> {
    val q = normalizeSearch(query)
    return list.filter { s ->
        filter.matches(s.status) && (q.isEmpty() ||
            normalizeSearch(s.matricula).contains(q) ||
            normalizeSearch(s.nombre).contains(q) ||
            normalizeSearch(s.carrera).contains(q))
    }.sortedBy { it.nombre.lowercase() }
}

fun normalizeSearch(s: String): String =
    java.text.Normalizer.normalize(s.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

data class StudentsUi(
    val loading: Boolean = true,
    val query: String = "",
    val filter: StudentFilter = StudentFilter.ALL,
    val students: List<Student> = emptyList(),
    val enrolled: Set<String> = emptySet(),
    val counts: Map<StudentFilter, Int> = emptyMap()
)

@HiltViewModel
class StudentsViewModel @Inject constructor(
    private val repo: StudentRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(StudentFilter.ALL)

    val ui: StateFlow<StudentsUi> = combine(repo.observeAll(), repo.observeEnrolled(), query, filter) { all, enrolled, q, f ->
        StudentsUi(
            loading = false,
            query = q,
            filter = f,
            students = filterStudents(all, q, f),
            enrolled = enrolled,
            counts = StudentFilter.entries.associateWith { sf -> all.count { sf.matches(it.status) } }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentsUi())

    fun setQuery(q: String) { query.value = q }
    fun setFilter(f: StudentFilter) { filter.value = f }
    fun approve(m: String) = viewModelScope.launch { repo.approve(m, auth.currentGuardName()) }
    fun reject(m: String) = viewModelScope.launch { repo.reject(m, auth.currentGuardName()) }
    fun delete(m: String) = viewModelScope.launch { repo.deleteStudentData(m) }
}

@Composable
fun StudentsSection(vm: StudentsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val actions = LocalAdminActions.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    StudentsContent(
        ui = ui,
        snackbar = snackbar,
        onQuery = vm::setQuery,
        onFilter = vm::setFilter,
        onApprove = { vm.approve(it.matricula); scope.launch { snackbar.showSnackbar("${it.nombre} aprobado") } },
        onReject = { vm.reject(it.matricula); scope.launch { snackbar.showSnackbar("Registro de ${it.nombre} rechazado") } },
        onDelete = { vm.delete(it.matricula); scope.launch { snackbar.showSnackbar("Datos biométricos de ${it.nombre} eliminados") } },
        onManualEntry = { actions.onManualEntry(it.matricula) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsContent(
    ui: StudentsUi,
    snackbar: SnackbarHostState? = null,
    onQuery: (String) -> Unit,
    onFilter: (StudentFilter) -> Unit,
    onApprove: (Student) -> Unit,
    onReject: (Student) -> Unit,
    onDelete: (Student) -> Unit,
    onManualEntry: (Student) -> Unit
) {
    var selected by remember { mutableStateOf<Student?>(null) }
    SectionScaffold(
        title = "Alumnos",
        subtitle = "${ui.counts[StudentFilter.ALL] ?: 0} registrados",
        snackbarHostState = snackbar
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = Spacing.screenCompact)) {
                AppSearchField(ui.query, onQuery, "Buscar alumno o matrícula", Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Spacing.sm))
            FilterRow(StudentFilter.entries, ui.filter, { it.label + (ui.counts[it]?.let { c -> " · $c" } ?: "") }, onFilter)
            when {
                ui.loading -> SkeletonList(modifier = Modifier.padding(horizontal = Spacing.screenCompact))
                ui.students.isEmpty() -> EmptyState(
                    title = if (ui.query.isBlank() && ui.filter == StudentFilter.ALL) "Aún no hay alumnos" else "Sin resultados",
                    message = if (ui.query.isBlank() && ui.filter == StudentFilter.ALL)
                        "Los alumnos aparecerán aquí cuando se registren desde la app."
                    else "Prueba con otra búsqueda o cambia el filtro.",
                    modifier = Modifier.fillMaxSize(),
                    illustration = if (ui.query.isNotBlank()) {
                        { IconBadge(Icons.Rounded.SearchOff, Tone.Neutral, size = 72.dp) }
                    } else {
                        { edu.utcj.acceso.ui.components.EmptyBoxIllustration(Modifier.height(140.dp).widthIn(max = 180.dp)) }
                    }
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                    items(ui.students, key = { it.matricula }) { s ->
                        AppListItem(
                            title = s.nombre,
                            subtitle = "${s.matricula}${if (s.carrera.isNotBlank()) " · ${s.carrera}" else ""}",
                            leading = { InitialsAvatar(s.nombre) },
                            trailing = {
                                Column(horizontalAlignment = Alignment.End) {
                                    StudentStatusChip(s.status)
                                    if (s.matricula !in ui.enrolled) {
                                        Spacer(Modifier.height(2.dp))
                                        Text("Sin rostro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            onClick = { selected = s },
                            modifier = Modifier.padding(horizontal = Spacing.sm)
                        )
                    }
                }
            }
        }
    }

    selected?.let { s ->
        StudentDetailSheet(
            student = s,
            enrolled = s.matricula in ui.enrolled,
            onDismiss = { selected = null },
            onApprove = { onApprove(s); selected = null },
            onReject = { onReject(s); selected = null },
            onDelete = { onDelete(s); selected = null },
            onManualEntry = { onManualEntry(s); selected = null }
        )
    }
}

@Composable
fun <T> FilterRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.screenCompact),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(options.size) { i ->
            val o = options[i]
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                label = { Text(label(o)) },
                leadingIcon = if (o == selected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.height(18.dp)) }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailSheet(
    student: Student,
    enrolled: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit,
    onManualEntry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReject by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding().padding(bottom = Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(student.nombre, size = 56.dp)
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(student.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(student.matricula, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StudentStatusChip(student.status)
            }
            Spacer(Modifier.height(Spacing.lg))
            DetailRow("Carrera", student.carrera.ifBlank { "—" }, icon = Icons.Rounded.School)
            DetailRow("Rostro registrado", if (enrolled) "Sí (cifrado)" else "No", icon = Icons.Rounded.Face)
            DetailRow("Registro", TimeUtil.formatShortDate(student.createdAtMs), icon = Icons.Rounded.CalendarMonth)
            DetailRow(
                "Consentimiento",
                if (student.consentTimestampMs > 0) "v${student.consentVersion} · ${TimeUtil.formatShortDate(student.consentTimestampMs)}" else "—",
                icon = Icons.Rounded.VerifiedUser
            )
            student.approvedByGuard?.let { g ->
                DetailRow("Revisado por", "$g${student.approvedAtMs?.let { " · " + TimeUtil.formatShortDate(it) } ?: ""}", icon = Icons.Rounded.Badge)
            }
            Spacer(Modifier.height(Spacing.xl))
            if (student.status == StudentStatus.PENDING || student.status == StudentStatus.REJECTED) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    PrimaryButton("Aprobar", onApprove, Modifier.weight(1f), icon = Icons.Rounded.Check)
                    if (student.status == StudentStatus.PENDING) {
                        SecondaryButton("Rechazar", { confirmReject = true }, Modifier.weight(1f), icon = Icons.Rounded.Block)
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
            SecondaryButton("Registrar entrada manual", onManualEntry, Modifier.fillMaxWidth(), icon = Icons.Rounded.PersonAddAlt1)
            Spacer(Modifier.height(Spacing.md))
            DangerButton("Eliminar datos biométricos", { confirmDelete = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.DeleteForever)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "¿Eliminar datos de ${student.nombre}?",
            message = "Se borrarán su registro y sus vectores biométricos. Tendrá que registrarse de nuevo. La bitácora se conserva.",
            confirmText = "Eliminar",
            destructive = true,
            icon = Icons.Rounded.DeleteForever,
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDelete() }
        )
    }
    if (confirmReject) {
        ConfirmDialog(
            title = "¿Rechazar registro?",
            message = "${student.nombre} no podrá entrar con reconocimiento facial hasta que se apruebe.",
            confirmText = "Rechazar",
            destructive = true,
            icon = Icons.Rounded.Block,
            onDismiss = { confirmReject = false },
            onConfirm = { confirmReject = false; onReject() }
        )
    }
}
