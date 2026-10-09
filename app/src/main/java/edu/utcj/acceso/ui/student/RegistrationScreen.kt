package edu.utcj.acceso.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.CenteredColumn
import edu.utcj.acceso.ui.components.PendingIllustration
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.StepIndicator
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Motion
import edu.utcj.acceso.ui.theme.Spacing

@Composable
fun RegistrationScreen(
    onGoHome: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onDeleteData: () -> Unit,
    vm: RegistrationViewModel = hiltViewModel()
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val goBack = { if (!vm.back()) onBack() }
    BackHandler(enabled = state.step == RegStep.CONSENT) { goBack() }
    BackHandler(enabled = state.step == RegStep.DONE) { onFinish() }

    RegistrationContent(
        state = state,
        onMatricula = vm::onMatricula,
        onNombre = vm::onNombre,
        onCarrera = vm::onCarrera,
        onCorreo = vm::onCorreo,
        onConsent = vm::onConsent,
        onNext = vm::next,
        onBack = { if (state.step == RegStep.DONE) onFinish() else goBack() },
        onGoHome = onGoHome,
        onFinish = onFinish,
        onDeleteData = onDeleteData
    )
}

@Composable
fun RegistrationContent(
    state: RegistrationUi,
    onMatricula: (String) -> Unit,
    onNombre: (String) -> Unit,
    onCarrera: (String) -> Unit,
    onCorreo: (String) -> Unit,
    onConsent: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onFinish: () -> Unit,
    onDeleteData: () -> Unit
) {
    val reduced = AppTheme.reducedMotion
    Scaffold(
        topBar = { AccesoTopBar(title = "Registro de alumno", subtitle = "Acceso con QR", onBack = onBack) },
        bottomBar = { if (state.step != RegStep.DONE) BottomActions(state, onNext) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            CenteredColumn {
                StepIndicator(
                    steps = RegStep.entries.map { it.label },
                    current = if (state.step == RegStep.DONE) RegStep.entries.size else state.step.ordinal,
                    modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.lg)
                )
            }
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    if (reduced) fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT))
                    else {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(tween(Motion.MEDIUM)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                            (slideOutHorizontally(tween(Motion.MEDIUM)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(Motion.SHORT)))
                    }
                },
                label = "regStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    RegStep.DATA -> DataStep(state, onMatricula, onNombre, onCarrera, onCorreo, onNext)
                    RegStep.CONSENT -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        CenteredColumn {
                            ExistingBanner(state.existingStatus)
                            ConsentContent(state.consentAccepted, onConsent, onDeleteData)
                            Spacer(Modifier.height(Spacing.lg))
                        }
                    }
                    RegStep.DONE -> DoneStep(state, onGoHome, onFinish)
                }
            }
        }
    }
}

@Composable
private fun BottomActions(state: RegistrationUi, onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            CenteredColumn(Modifier.navigationBarsPadding().padding(vertical = Spacing.md)) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(Spacing.sm))
                }
                when (state.step) {
                    RegStep.DATA -> PrimaryButton(
                        "Continuar", onClick = onNext, icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        loading = state.checking, modifier = Modifier.fillMaxWidth()
                    )
                    RegStep.CONSENT -> PrimaryButton(
                        "Acepto y generar mi QR", onClick = onNext, enabled = state.consentAccepted,
                        loading = state.saving, icon = Icons.Rounded.QrCode2, modifier = Modifier.fillMaxWidth()
                    )
                    RegStep.DONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun DataStep(
    state: RegistrationUi,
    onMatricula: (String) -> Unit,
    onNombre: (String) -> Unit,
    onCarrera: (String) -> Unit,
    onCorreo: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CenteredColumn(maxWidth = Spacing.formMaxWidth) {
            Text("Cuéntanos quién eres", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Usa tus datos oficiales; seguridad los validará contra el estatus institucional.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))
            AppTextField(
                value = state.matricula, onValueChange = onMatricula, label = "Matrícula",
                leadingIcon = Icons.Rounded.Badge, keyboardType = KeyboardType.Ascii,
                isError = state.matriculaError != null,
                supportingText = state.matriculaError ?: "Ejemplo: 20210001"
            )
            Spacer(Modifier.height(Spacing.sm))
            AppTextField(
                value = state.nombre, onValueChange = onNombre, label = "Nombre completo",
                leadingIcon = Icons.Rounded.Person,
                isError = state.nombreError != null,
                supportingText = state.nombreError
            )
            Spacer(Modifier.height(Spacing.sm))
            AppTextField(
                value = state.carrera, onValueChange = onCarrera, label = "Carrera y grupo",
                leadingIcon = Icons.Rounded.School, supportingText = "Ejemplo: TI · 5A"
            )
            Spacer(Modifier.height(Spacing.sm))
            AppTextField(
                value = state.correo, onValueChange = onCorreo, label = "Correo (opcional)",
                leadingIcon = Icons.Rounded.Email, keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done, onImeAction = onNext,
                isError = state.correoError != null,
                supportingText = state.correoError
            )
            Spacer(Modifier.height(Spacing.xl))
            AlertBanner(
                title = "Tu registro queda pendiente",
                message = "Al terminar verás tu QR de registro. Muéstraselo al guardia una sola vez para activar tu acceso.",
                tone = Tone.Info,
                icon = Icons.Rounded.Info
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun ExistingBanner(existing: StudentStatus?) {
    if (existing == null) return
    val blocked = existing == StudentStatus.BAJA || existing == StudentStatus.SUSPENDIDO
    AlertBanner(
        title = if (blocked) "Estatus institucional: ${existing.labelEs()}" else "Ya tienes un registro (${existing.labelEs()})",
        message = if (blocked) "Puedes registrarte, pero el acceso seguirá bloqueado hasta que tu estatus cambie."
        else "Si continúas se creará una llave nueva en este teléfono y deberás mostrar tu nuevo QR de registro al guardia.",
        tone = Tone.Warning,
        icon = Icons.Rounded.Warning,
        modifier = Modifier.padding(bottom = Spacing.lg)
    )
}

@Composable
private fun DoneStep(state: RegistrationUi, onGoHome: () -> Unit, onFinish: () -> Unit) {
    val blocked = state.savedStatus == StudentStatus.BAJA || state.savedStatus == StudentStatus.SUSPENDIDO
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CenteredColumn(maxWidth = Spacing.formMaxWidth) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PendingIllustration(Modifier.size(168.dp))
                Spacer(Modifier.height(Spacing.lg))
                Text("¡Tu QR de registro está listo!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.sm))
                StatusPill(
                    if (blocked) "Estatus ${state.savedStatus?.labelEs()}" else "Pendiente de aprobación",
                    if (blocked) Tone.Neutral else Tone.Warning
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Tus datos y tu llave segura se guardaron en este teléfono. No usamos fotos ni biometría.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            AppCard {
                Text("¿Qué sigue?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.sm))
                listOf(
                    "Ve a caseta y muestra tu QR de registro al guardia.",
                    "El guardia lo escanea y aprueba tu acceso.",
                    "Después, en cada entrada, muestra tu QR de acceso: cambia solo y vence en minutos."
                ).forEachIndexed { i, t ->
                    Row(Modifier.padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(Spacing.md))
                        Text(t, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            PrimaryButton("Ver mi QR de registro", onClick = onGoHome, icon = Icons.Rounded.QrCode2, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton("Terminar", onClick = onFinish, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "Tu QR solo funciona en este teléfono. Para volver a verlo entra a «Soy alumno» → «Ya tengo registro».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            )
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}
