package edu.utcj.acceso.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.AccessLogRepository
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.validation.RegistrationValidator
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.CenteredColumn
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.StudentStatusChip
import edu.utcj.acceso.ui.components.SuccessIllustration
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val log: AccessLogRepository,
    private val auth: AuthRepository,
    private val students: StudentRepository
) : ViewModel() {
    val guardName: String get() = auth.currentGuardName()
    suspend fun lookup(m: String): Student? = students.get(RegistrationValidator.normalizeMatricula(m))
    suspend fun log(mat: String, nombre: String, reason: String) {
        log.log(
            RegistrationValidator.normalizeMatricula(mat), nombre,
            AccessResult.MANUAL, AccessMethod.MANUAL,
            guard = auth.currentGuardName(),
            reason = reason
        )
    }
}

private val quickReasons = listOf(
    "Falla de reconocimiento", "Credencial física verificada", "Sin teléfono para QR", "Evento autorizado", "Proveedor / personal"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryScreen(
    initialMatricula: String?,
    onBack: () -> Unit,
    vm: ManualEntryViewModel = hiltViewModel()
) {
    var matricula by remember { mutableStateOf(initialMatricula.orEmpty()) }
    var nombre by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<Student?>(null) }
    var searched by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lookup: () -> Unit = {
        scope.launch {
            found = vm.lookup(matricula)
            searched = true
            found?.let { nombre = it.nombre }
        }
    }
    LaunchedEffect(initialMatricula) { if (!initialMatricula.isNullOrBlank()) lookup() }

    Scaffold(topBar = { AccesoTopBar(title = "Entrada manual", subtitle = "Autoriza: ${vm.guardName}", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            CenteredColumn(maxWidth = Spacing.formMaxWidth) {
                if (done) {
                    Column(Modifier.fillMaxWidth().padding(top = Spacing.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
                        SuccessIllustration(Modifier.size(150.dp))
                        Spacer(Modifier.height(Spacing.lg))
                        Text("Entrada registrada", style = MaterialTheme.typography.headlineSmall)
                        Text("$nombre · ${RegistrationValidator.normalizeMatricula(matricula)}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(Spacing.xl))
                        PrimaryButton("Registrar otra", onClick = {
                            matricula = ""; nombre = ""; reason = ""; found = null; searched = false; done = false
                        }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(Spacing.sm))
                        androidx.compose.material3.TextButton(onClick = onBack) { Text("Volver al panel") }
                    }
                    return@CenteredColumn
                }
                Spacer(Modifier.height(Spacing.md))
                AlertBanner(
                    "Queda registrado en la bitácora con tu nombre",
                    Tone.Info, Icons.Rounded.Badge,
                    message = "Úsalo solo cuando hayas verificado la identidad de la persona."
                )
                Spacer(Modifier.height(Spacing.lg))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(
                        matricula, { matricula = it; searched = false; found = null }, "Matrícula",
                        modifier = Modifier.weight(1f), leadingIcon = Icons.Rounded.Badge,
                        keyboardType = KeyboardType.Ascii, onImeAction = lookup
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    IconButton(onClick = lookup, enabled = matricula.isNotBlank()) {
                        Icon(Icons.Rounded.Search, contentDescription = "Buscar alumno")
                    }
                }
                AnimatedVisibility(searched) {
                    Column {
                        Spacer(Modifier.height(Spacing.sm))
                        val f = found
                        if (f != null) {
                            AppCard(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InitialsAvatar(f.nombre)
                                    Spacer(Modifier.width(Spacing.md))
                                    Column(Modifier.weight(1f)) {
                                        Text(f.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text(f.carrera.ifBlank { f.matricula }, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StudentStatusChip(f.status)
                                }
                            }
                            if (!f.status.allowsAccess()) {
                                Spacer(Modifier.height(Spacing.sm))
                                AlertBanner("Estatus sin acceso automático", Tone.Warning, Icons.Rounded.WarningAmber,
                                    message = "Confirma con control escolar antes de autorizar.")
                            }
                        } else {
                            Text("Matrícula no registrada en la app. Puedes capturar el nombre manualmente.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                AppTextField(nombre, { nombre = it }, "Nombre", leadingIcon = Icons.Rounded.Person)
                Spacer(Modifier.height(Spacing.lg))
                Text("Motivo", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xs))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    quickReasons.forEach { r ->
                        FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r) },
                            leadingIcon = if (reason == r) { { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) } } else null)
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                AppTextField(reason, { reason = it }, "Detalle del motivo (obligatorio)", singleLine = false, minLines = 2)
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton(
                    "Registrar entrada",
                    onClick = {
                        scope.launch {
                            saving = true
                            vm.log(matricula, nombre.trim(), reason.trim())
                            saving = false
                            done = true
                        }
                    },
                    enabled = RegistrationValidator.matriculaError(matricula) == null && nombre.isNotBlank() && reason.isNotBlank(),
                    loading = saving,
                    icon = Icons.Rounded.Check,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}
