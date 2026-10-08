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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.CameraPermissionGate
import edu.utcj.acceso.ui.components.CameraPreview
import edu.utcj.acceso.ui.components.CenteredColumn
import edu.utcj.acceso.ui.components.FaceGuideOverlay
import edu.utcj.acceso.ui.components.GuidanceChip
import edu.utcj.acceso.ui.components.PendingIllustration
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SampleDots
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.StepIndicator
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.components.screenHorizontalPadding
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Motion
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.isCompactHeight
import edu.utcj.acceso.util.isLandscape
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(
    onGoHome: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onDeleteData: () -> Unit,
    vm: RegistrationViewModel = hiltViewModel()
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val goBack = { if (!vm.back()) onBack() }
    BackHandler(enabled = state.step == RegStep.CONSENT || state.step == RegStep.CAPTURE) { goBack() }
    BackHandler(enabled = state.step == RegStep.DONE) { onFinish() }

    RegistrationContent(
        state = state,
        onMatricula = vm::onMatricula,
        onNombre = vm::onNombre,
        onCarrera = vm::onCarrera,
        onConsent = vm::onConsent,
        onNext = vm::next,
        onBack = { if (state.step == RegStep.DONE) onFinish() else goBack() },
        onRetake = vm::resetCapture,
        onGoHome = { vm.rememberOnDevice(); onGoHome() },
        onFinish = onFinish,
        onDeleteData = onDeleteData,
        cameraContent = {
            CameraPermissionGate(onDark = true) {
                CameraPreview { bmp ->
                    if (!state.capturing && state.samples.size < state.maxSamples) {
                        scope.launch { vm.onFrame(bmp) }
                    }
                }
            }
        }
    )
}

@Composable
fun RegistrationContent(
    state: RegistrationUi,
    onMatricula: (String) -> Unit,
    onNombre: (String) -> Unit,
    onCarrera: (String) -> Unit,
    onConsent: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onGoHome: () -> Unit,
    onFinish: () -> Unit,
    onDeleteData: () -> Unit,
    cameraContent: @Composable () -> Unit
) {
    val reduced = AppTheme.reducedMotion
    Scaffold(
        topBar = {
            AccesoTopBar(
                title = "Registro de alumno",
                subtitle = if (state.step == RegStep.CAPTURE) "Muestras ${state.samples.size} de ${state.maxSamples}" else "Acceso facial",
                onBack = onBack
            )
        },
        bottomBar = {
            if (state.step != RegStep.DONE) {
                BottomActions(state, onNext, onRetake)
            }
        },
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
                    RegStep.DATA -> DataStep(state, onMatricula, onNombre, onCarrera, onNext)
                    RegStep.CONSENT -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        CenteredColumn {
                            ExistingBanner(state.existingStatus)
                            ConsentContent(state.consentAccepted, onConsent, onDeleteData)
                            Spacer(Modifier.height(Spacing.lg))
                        }
                    }
                    RegStep.CAPTURE -> CaptureStep(state, cameraContent)
                    RegStep.DONE -> DoneStep(state, onGoHome, onFinish)
                }
            }
        }
    }
}

@Composable
private fun BottomActions(state: RegistrationUi, onNext: () -> Unit, onRetake: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            CenteredColumn(Modifier.navigationBarsPadding().padding(vertical = Spacing.md)) {
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(Spacing.sm))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    when (state.step) {
                        RegStep.DATA -> PrimaryButton(
                            "Continuar", onClick = onNext, icon = Icons.AutoMirrored.Rounded.ArrowForward,
                            loading = state.checking, modifier = Modifier.fillMaxWidth()
                        )
                        RegStep.CONSENT -> PrimaryButton(
                            "Acepto y continuar", onClick = onNext, enabled = state.consentAccepted,
                            icon = Icons.AutoMirrored.Rounded.ArrowForward, modifier = Modifier.fillMaxWidth()
                        )
                        RegStep.CAPTURE -> {
                            SecondaryButton(
                                "Reiniciar", onClick = onRetake, icon = Icons.Rounded.Refresh,
                                enabled = state.samples.isNotEmpty() && !state.saving
                            )
                            PrimaryButton(
                                text = when {
                                    state.saving -> "Guardando…"
                                    state.canSave -> "Guardar registro"
                                    else -> "Capturando ${state.samples.size}/${state.minSamples}"
                                },
                                onClick = onNext,
                                enabled = state.canSave,
                                loading = state.saving,
                                icon = Icons.Rounded.Save,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        RegStep.DONE -> Unit
                    }
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
                value = state.carrera, onValueChange = onCarrera, label = "Carrera (opcional)",
                leadingIcon = Icons.Rounded.School, imeAction = ImeAction.Done, onImeAction = onNext
            )
            Spacer(Modifier.height(Spacing.xl))
            AlertBanner(
                title = "Tu registro queda pendiente",
                message = "Un guardia revisará y aprobará tu registro antes de que el kiosco te permita el acceso.",
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
        else "Si continúas, tus muestras faciales se reemplazarán y el registro volverá a revisión.",
        tone = Tone.Warning,
        icon = Icons.Rounded.Warning,
        modifier = Modifier.padding(bottom = Spacing.lg)
    )
}

@Composable
private fun CaptureStep(state: RegistrationUi, cameraContent: @Composable () -> Unit) {
    val side = isLandscape() || isCompactHeight()
    val camera: @Composable (Modifier) -> Unit = { m ->
        Box(m.clip(MaterialTheme.shapes.extraLarge).background(androidx.compose.ui.graphics.Color.Black)) {
            cameraContent()
            FaceGuideOverlay(
                status = state.faceStatus,
                progress = state.samples.size / state.maxSamples.toFloat()
            )
            GuidanceChip(
                state.guidance,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.lg, start = Spacing.lg, end = Spacing.lg)
            )
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SampleDots(state.samples.size, state.maxSamples)
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "${state.samples.size} de ${state.maxSamples} muestras",
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
    val hp = screenHorizontalPadding()
    if (side) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = hp, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            camera(Modifier.weight(1.3f).fillMaxHeight())
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { CaptureTips() }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = hp)) {
            camera(Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(Spacing.md))
            CaptureTips(compact = true)
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun CaptureTips(compact: Boolean = false) {
    val tips: List<Pair<ImageVector, String>> = if (compact) listOf(
        Icons.Rounded.LightMode to "Buena luz",
        Icons.Rounded.VisibilityOff to "Sin lentes",
        Icons.Rounded.Visibility to "De frente"
    ) else listOf(
        Icons.Rounded.LightMode to "Buena luz, sin contraluz",
        Icons.Rounded.VisibilityOff to "Sin lentes oscuros ni gorra",
        Icons.Rounded.Visibility to "Mira al frente, rostro completo"
    )
    if (compact) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            tips.forEach { (icon, text) ->
                Row(
                    Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    } else {
        Text("Consejos para una buena captura", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.md))
        tips.forEach { (icon, text) ->
            Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Spacing.md))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Mantén tu rostro dentro del óvalo. La captura es automática: no se guardan fotos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DoneStep(state: RegistrationUi, onGoHome: () -> Unit, onFinish: () -> Unit) {
    val blocked = state.savedStatus == StudentStatus.BAJA || state.savedStatus == StudentStatus.SUSPENDIDO
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CenteredColumn(maxWidth = Spacing.formMaxWidth) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                PendingIllustration(Modifier.size(168.dp))
                Spacer(Modifier.height(Spacing.lg))
                Text("¡Registro enviado!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.sm))
                StatusPill(
                    if (blocked) "Estatus ${state.savedStatus?.labelEs()}" else "Pendiente de aprobación",
                    if (blocked) Tone.Neutral else Tone.Warning
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Guardamos ${state.samples.size} vectores biométricos cifrados y ninguna fotografía.",
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
                    "Seguridad revisa y aprueba tu registro.",
                    "Colócate frente al kiosco de acceso y mira al óvalo.",
                    "Si el rostro falla, muestra tu QR dinámico o usa huella."
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
            PrimaryButton("Ir a mi inicio", onClick = onGoHome, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton("Terminar", onClick = onFinish, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "«Ir a mi inicio» recuerda tu matrícula en este dispositivo. Si es un equipo compartido, elige «Terminar».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)
            )
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}
