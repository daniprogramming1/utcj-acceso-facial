package edu.utcj.acceso.ui.admin

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.ConfirmDialog
import edu.utcj.acceso.ui.components.EmptyState
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.SkeletonList
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.SuccessIllustration
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApprovalsUi(
    val loading: Boolean = true,
    val pending: List<Student> = emptyList(),
    val enrolled: Set<String> = emptySet()
)

@HiltViewModel
class ApprovalsViewModel @Inject constructor(
    private val students: StudentRepository,
    private val auth: AuthRepository
) : ViewModel() {
    val ui: StateFlow<ApprovalsUi> = combine(students.observePending(), students.observeEnrolled()) { p, e ->
        ApprovalsUi(false, p.sortedBy { it.createdAtMs }, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApprovalsUi())

    fun approve(m: String) = viewModelScope.launch { students.approve(m, auth.currentGuardName()) }
    fun reject(m: String) = viewModelScope.launch { students.reject(m, auth.currentGuardName()) }
}

@Composable
fun ApprovalsSection(vm: ApprovalsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    ApprovalsContent(
        ui = ui,
        snackbar = snackbar,
        onApprove = { vm.approve(it.matricula); scope.launch { snackbar.showSnackbar("${it.nombre} ya puede entrar") } },
        onReject = { vm.reject(it.matricula); scope.launch { snackbar.showSnackbar("Registro rechazado") } }
    )
}

@Composable
fun ApprovalsContent(
    ui: ApprovalsUi,
    snackbar: SnackbarHostState? = null,
    onApprove: (Student) -> Unit,
    onReject: (Student) -> Unit
) {
    var toReject by remember { mutableStateOf<Student?>(null) }
    SectionScaffold(
        title = "Aprobaciones",
        subtitle = if (ui.pending.isEmpty()) "Sin solicitudes" else "${ui.pending.size} por revisar",
        snackbarHostState = snackbar
    ) { padding ->
        when {
            ui.loading -> SkeletonList(modifier = Modifier.padding(padding).padding(horizontal = Spacing.screenCompact))
            ui.pending.isEmpty() -> EmptyState(
                title = "Todo al día",
                message = "No hay registros pendientes de aprobación. Te avisaremos con un indicador en el menú.",
                illustration = { SuccessIllustration(Modifier.size(150.dp)) },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = Spacing.screenCompact, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(ui.pending, key = { it.matricula }) { s ->
                    AppCard(Modifier.animateContentSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(s.nombre, size = 48.dp)
                            Spacer(Modifier.width(Spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(s.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    "${s.matricula}${if (s.carrera.isNotBlank()) " · ${s.carrera}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            StatusPill(
                                if (s.matricula in ui.enrolled) "Rostro capturado" else "Sin rostro",
                                if (s.matricula in ui.enrolled) Tone.Success else Tone.Warning,
                                icon = Icons.Rounded.Face
                            )
                            if (s.consentTimestampMs > 0) StatusPill("Consentimiento", Tone.Info, icon = Icons.Rounded.VerifiedUser)
                        }
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "Solicitado ${TimeUtil.relative(s.createdAtMs).lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SecondaryButton("Rechazar", { toReject = s }, Modifier.weight(1f), icon = Icons.Rounded.Block)
                            PrimaryButton("Aprobar", { onApprove(s) }, Modifier.weight(1f), icon = Icons.Rounded.Check)
                        }
                    }
                }
            }
        }
    }
    toReject?.let { s ->
        ConfirmDialog(
            title = "¿Rechazar a ${s.nombre}?",
            message = "No podrá entrar con reconocimiento facial. Puedes aprobarlo más tarde desde Alumnos.",
            confirmText = "Rechazar",
            destructive = true,
            icon = Icons.Rounded.Block,
            onDismiss = { toReject = null },
            onConfirm = { toReject = null; onReject(s) }
        )
    }
}
