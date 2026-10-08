package edu.utcj.acceso.ui.student

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.HowToReg
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
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.CenteredColumn
import edu.utcj.acceso.ui.components.LinkButton
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.StudentIllustration
import edu.utcj.acceso.ui.theme.Spacing
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentAccessViewModel @Inject constructor(
    private val students: StudentRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    /** @return mensaje de error o null si se abrió correctamente. */
    suspend fun open(raw: String): String? {
        RegistrationValidator.matriculaError(raw)?.let { return it }
        val mat = RegistrationValidator.normalizeMatricula(raw)
        students.get(mat) ?: return "No encontramos un registro con esa matrícula"
        settings.setRememberedStudent(mat)
        return null
    }
}

@Composable
fun StudentAccessScreen(
    onOpenHome: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    vm: StudentAccessViewModel = hiltViewModel()
) {
    var matricula by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val submit: () -> Unit = {
        scope.launch {
            loading = true
            error = vm.open(matricula)
            loading = false
            if (error == null) onOpenHome()
        }
    }
    Scaffold(topBar = { AccesoTopBar(title = "Consultar mi acceso", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            CenteredColumn(maxWidth = Spacing.formMaxWidth) {
                Spacer(Modifier.height(Spacing.xl))
                StudentIllustration(Modifier.size(120.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    "Ingresa tu matrícula",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Consulta tu estatus de aprobación y genera tu QR dinámico de respaldo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.xl))
                AppTextField(
                    value = matricula,
                    onValueChange = { matricula = it; error = null },
                    label = "Matrícula",
                    leadingIcon = Icons.Rounded.Badge,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                    onImeAction = submit,
                    isError = error != null,
                    supportingText = error
                )
                Spacer(Modifier.height(Spacing.lg))
                PrimaryButton(
                    "Continuar", onClick = submit, enabled = matricula.isNotBlank(), loading = loading,
                    icon = Icons.AutoMirrored.Rounded.ArrowForward, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.sm))
                LinkButton(
                    "¿Aún no te registras? Regístrate aquí", onClick = onRegister, icon = Icons.Rounded.HowToReg,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
