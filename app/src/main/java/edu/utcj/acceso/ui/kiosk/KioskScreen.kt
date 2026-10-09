package edu.utcj.acceso.ui.kiosk

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.SyncUiState
import edu.utcj.acceso.ui.components.BrandLogoTile
import edu.utcj.acceso.ui.components.CameraPermissionGate
import edu.utcj.acceso.ui.components.GuidanceChip
import edu.utcj.acceso.ui.components.QrScannerCamera
import edu.utcj.acceso.ui.components.ScanFrameOverlay
import edu.utcj.acceso.ui.components.SyncStatusPill
import edu.utcj.acceso.ui.theme.KioskType
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.delay

/** Colores fijos del kiosco (siempre oscuro para alto contraste en vestíbulos). */
internal object KioskColors {
    val background = Color(0xFF07152A)
    val panel = Color(0xFF0D213D)
    val panelBorder = Color.White.copy(alpha = 0.08f)
    val textSecondary = Color.White.copy(alpha = 0.72f)
}

@Composable
fun KioskScreen(
    onExitRequest: () -> Unit,
    vm: KioskViewModel = hiltViewModel()
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val view = LocalView.current

    KioskWindowEffect(vm.orientation)
    DisposableEffect(vm) {
        vm.setActive(true)
        onDispose { vm.setActive(false) }
    }
    BackHandler { onExitRequest() }

    // Reloj
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L - (now % 1_000L))
        }
    }

    // Resultado: háptica, sonido y regreso automático
    val totalSeconds = (vm.idleMs / 1000L).toInt().coerceAtLeast(1)
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(state.result) {
        val r = state.result ?: return@LaunchedEffect
        val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (r.allowed) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        } else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(feedback)
        if (vm.soundEnabled) {
            tone?.startTone(if (r.allowed) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 220)
        }
        secondsLeft = totalSeconds
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
        vm.clearResult()
    }

    KioskContent(
        state = state,
        nowMs = now,
        sync = sync,
        secondsLeft = secondsLeft,
        totalSeconds = totalSeconds,
        onCallGuard = vm::requestAssistance,
        onDismissResult = vm::clearResult,
        onExit = onExitRequest,
        cameraContent = {
            CameraPermissionGate(onDark = true) {
                // Cámara frontal: el kiosco mira a los alumnos (con respaldo a la trasera).
                QrScannerCamera(Modifier.fillMaxSize(), front = true, paused = state.result != null, onCode = vm::onCode)
            }
        }
    )
}

/** Pantalla completa inmersiva, pantalla siempre encendida y orientación configurada. */
@Composable
private fun KioskWindowEffect(orientation: SettingsRepository.KioskOrientation) {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(orientation) {
        val activity = context as? Activity
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.requestedOrientation = when (orientation) {
            SettingsRepository.KioskOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            SettingsRepository.KioskOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            SettingsRepository.KioskOrientation.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).let { c ->
                c.hide(WindowInsetsCompat.Type.systemBars())
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/**
 * Interfaz del kiosco sin dependencias de Hilt/cámara (usada también por las pruebas de
 * capturas). [cameraContent] recibe la vista previa real o un marcador.
 */
@Composable
fun KioskContent(
    state: KioskViewModel.Ui,
    nowMs: Long,
    sync: SyncUiState?,
    secondsLeft: Int,
    totalSeconds: Int,
    onCallGuard: () -> Unit,
    onDismissResult: () -> Unit,
    onExit: () -> Unit,
    cameraContent: @Composable () -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KioskColors.background, BrandConfig.palette.navy)))
    ) {
        val landscape = maxWidth > maxHeight
        val pad = if (maxWidth > 900.dp) 32.dp else 20.dp
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.spacedBy(pad)) {
                KioskCamera(state, cameraContent, Modifier.weight(1.3f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    KioskHeader(nowMs, sync, stacked = true, onExit = onExit)
                    Spacer(Modifier.weight(1f))
                    KioskPrompt(state, Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    KioskHelp(onCallGuard)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(pad)) {
                KioskHeader(nowMs, sync, stacked = false, onExit = onExit)
                Spacer(Modifier.height(Spacing.lg))
                KioskCamera(state, cameraContent, Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(Spacing.lg))
                KioskPrompt(state, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.lg))
                KioskHelp(onCallGuard)
            }
        }

        AnimatedVisibility(
            visible = state.result != null,
            enter = fadeIn() + scaleIn(initialScale = 1.04f),
            exit = fadeOut()
        ) {
            val r = state.result
            if (r != null) {
                KioskResultOverlay(
                    result = r,
                    secondsLeft = secondsLeft,
                    totalSeconds = totalSeconds,
                    assistanceRequested = state.assistanceRequested,
                    onCallGuard = onCallGuard,
                    onDismiss = onDismissResult
                )
            }
        }
    }
}

@Composable
private fun KioskHeader(nowMs: Long, sync: SyncUiState?, stacked: Boolean, onExit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            BrandLogoTile(size = 52.dp)
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(BrandConfig.APP_NAME, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1)
                Text(
                    BrandConfig.SECURITY_DESK_LABEL, style = MaterialTheme.typography.bodySmall,
                    color = KioskColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onExit) {
            Icon(Icons.Rounded.Lock, contentDescription = "Salir del modo kiosco (requiere contraseña)", tint = KioskColors.textSecondary)
        }
    }
    Spacer(Modifier.height(Spacing.lg))
    if (stacked) {
        Text(TimeUtil.formatTime(nowMs), style = KioskType.clock, color = Color.White)
        Text(
            TimeUtil.formatLongDate(nowMs).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium, color = KioskColors.textSecondary
        )
        Spacer(Modifier.height(Spacing.md))
        SyncStatusPill(sync, onDark = true)
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(TimeUtil.formatTime(nowMs), style = KioskType.clock, color = Color.White)
                Text(
                    TimeUtil.formatLongDate(nowMs).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall, color = KioskColors.textSecondary
                )
            }
            SyncStatusPill(sync, onDark = true)
        }
    }
}

@Composable
private fun KioskCamera(state: KioskViewModel.Ui, cameraContent: @Composable () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .clip(shape)
            .background(Color.Black)
            .border(1.dp, KioskColors.panelBorder, shape)
    ) {
        cameraContent()
        ScanFrameOverlay(frameFraction = 0.62f, animate = state.result == null)
        GuidanceChip(
            if (state.busy) "Verificando…" else "Coloca tu QR dentro del recuadro",
            large = true,
            icon = Icons.Rounded.QrCodeScanner,
            modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.xl)
        )
    }
}

@Composable
private fun KioskPrompt(state: KioskViewModel.Ui, modifier: Modifier) {
    val (title, subtitle) = if (state.busy) "Verificando…" to "Comprobando la firma y la vigencia de tu QR."
    else "Muestra tu QR de acceso" to "Abre «Mi acceso» en tu teléfono y acércalo a la cámara."
    AnimatedContent(
        targetState = title to subtitle,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "kioskPrompt",
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) { (t, s) ->
        Column {
            Text(t, style = KioskType.title, color = Color.White)
            Spacer(Modifier.height(Spacing.sm))
            Text(s, style = KioskType.body, color = KioskColors.textSecondary)
        }
    }
}

@Composable
private fun KioskHelp(onCallGuard: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandConfig.palette.primaryLight, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "Sin fotos ni biometría · QR firmado de un solo uso",
                style = MaterialTheme.typography.bodySmall,
                color = KioskColors.textSecondary
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text("¿No tienes tu QR o no funciona?", style = MaterialTheme.typography.labelLarge, color = KioskColors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        KioskOptionButton(Icons.Rounded.SupportAgent, "Llamar al guardia", onCallGuard, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun KioskOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = KioskColors.panel,
    content: Color = Color.White
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = container,
        contentColor = content,
        border = androidx.compose.foundation.BorderStroke(1.dp, KioskColors.panelBorder)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(Spacing.md))
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

