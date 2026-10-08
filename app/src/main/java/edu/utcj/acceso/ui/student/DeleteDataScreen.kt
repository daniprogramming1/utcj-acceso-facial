package edu.utcj.acceso.ui.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.validation.RegistrationValidator
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.CenteredColumn
import edu.utcj.acceso.ui.components.ConfirmDialog
import edu.utcj.acceso.ui.components.DangerButton
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SuccessIllustration
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteDataViewModel @Inject constructor(
    private val students: StudentRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    suspend fun delete(raw: String) {
        val matricula = RegistrationValidator.normalizeMatricula(raw)
        students.deleteStudentData(matricula)
        if (settings.getRememberedStudent() == matricula) settings.setRememberedStudent(null)
    }
}

@Composable
fun DeleteDataScreen(
    initialMatricula: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: DeleteDataViewModel = hiltViewModel()
) {
    var matricula by remember { mutableStateOf(initialMatricula) }
    var confirm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { AccesoTopBar(title = "Eliminar mis datos", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            CenteredColumn(maxWidth = Spacing.formMaxWidth) {
                AnimatedContent(targetState = done, label = "deleteDone") { isDone ->
                    if (isDone) {
                        Column(Modifier.fillMaxWidth().padding(top = Spacing.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
                            SuccessIllustration(Modifier.size(160.dp))
                            Spacer(Modifier.height(Spacing.lg))
                            Text("Datos eliminados", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                "Si existía un registro con esa matrícula, se borraron tu información y tus vectores biométricos. El kiosco ya no podrá reconocerte.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(Spacing.xl))
                            PrimaryButton("Volver al inicio", onClick = onDone, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        Column(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(Spacing.lg))
                            IconBadge(Icons.Rounded.DeleteForever, Tone.Danger, size = 56.dp)
                            Spacer(Modifier.height(Spacing.lg))
                            Text("Ejerce tu derecho de cancelación", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "Esta acción es inmediata y no se puede deshacer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(Spacing.lg))
                            AppCard {
                                Text("Se eliminará:", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(Spacing.sm))
                                listOf(
                                    Icons.Rounded.Person to "Tu registro de alumno y consentimiento",
                                    Icons.Rounded.EnhancedEncryption to "Todos tus vectores biométricos cifrados",
                                    Icons.Rounded.QrCode2 to "La posibilidad de generar tu QR dinámico"
                                ).forEach { (icon, text) ->
                                    Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(Spacing.md))
                                        Text(text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    "La bitácora de accesos se conserva por seguridad institucional.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(Spacing.xl))
                            AppTextField(
                                value = matricula, onValueChange = { matricula = it }, label = "Confirma tu matrícula",
                                leadingIcon = Icons.Rounded.Badge, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done
                            )
                            Spacer(Modifier.height(Spacing.lg))
                            DangerButton(
                                "Eliminar definitivamente", onClick = { confirm = true },
                                enabled = RegistrationValidator.matriculaError(matricula) == null,
                                loading = deleting, icon = Icons.Rounded.DeleteForever, modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = "¿Eliminar tus datos?",
            message = "Se borrarán el registro y los vectores biométricos de la matrícula ${RegistrationValidator.normalizeMatricula(matricula)}.",
            confirmText = "Eliminar",
            destructive = true,
            icon = Icons.Rounded.DeleteForever,
            onDismiss = { confirm = false },
            onConfirm = {
                confirm = false
                scope.launch {
                    deleting = true
                    vm.delete(matricula)
                    deleting = false
                    done = true
                }
            }
        )
    }
}
