package edu.utcj.acceso.ui.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.utcj.acceso.ui.admin.LocalAdminActions
import edu.utcj.acceso.ui.admin.SectionScaffold
import edu.utcj.acceso.ui.components.CameraPermissionGate
import edu.utcj.acceso.ui.components.GuidanceChip
import edu.utcj.acceso.ui.components.QrScannerCamera
import edu.utcj.acceso.ui.components.ScanFrameOverlay
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing

/** Sección «Escanear» del panel del guardia. */
@Composable
fun ScanSection(vm: ScanViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val actions = LocalAdminActions.current
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    DisposableEffect(vm) {
        vm.setActive(true)
        onDispose { vm.setActive(false) }
    }
    ScanContent(
        ui = ui,
        snackbar = snackbar,
        onRegisterEntry = vm::registerEntry,
        onDeny = vm::denyEntry,
        onApprove = vm::approveRegistration,
        onReject = vm::rejectRegistration,
        onDismiss = vm::dismiss,
        onManualEntry = { actions.onManualEntry(it) },
        cameraContent = {
            CameraPermissionGate(onDark = true) {
                QrScannerCamera(paused = ui.result != null, onCode = vm::onCode)
            }
        }
    )
}

@Composable
fun ScanContent(
    ui: ScanUi,
    snackbar: SnackbarHostState? = null,
    onRegisterEntry: () -> Unit,
    onDeny: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
    onManualEntry: ((String) -> Unit)?,
    cameraContent: @Composable () -> Unit
) {
    SectionScaffold(title = "Escanear QR", subtitle = "Acceso y registro de alumnos", snackbarHostState = snackbar) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 720.dp
            val camera: @Composable (Modifier) -> Unit = { m -> ScannerViewport(ui, cameraContent, m) }
            val panel: @Composable (Modifier) -> Unit = { m ->
                ResultPanel(ui, onRegisterEntry, onDeny, onApprove, onReject, onDismiss, onManualEntry, m)
            }
            if (wide) {
                Row(Modifier.fillMaxSize().padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    camera(Modifier.weight(1f).fillMaxHeight())
                    panel(Modifier.width(420.dp).fillMaxHeight().verticalScroll(rememberScrollState()))
                }
            } else {
                val hasResult = ui.result != null
                Column(Modifier.fillMaxSize().padding(horizontal = Spacing.screenCompact, vertical = Spacing.sm)) {
                    camera(
                        if (hasResult) Modifier.fillMaxWidth().heightIn(max = 180.dp).weight(0.35f, fill = false)
                        else Modifier.fillMaxWidth().weight(1f)
                    )
                    panel(
                        Modifier.fillMaxWidth().padding(top = Spacing.md)
                            .then(if (hasResult) Modifier.weight(1f) else Modifier)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerViewport(ui: ScanUi, cameraContent: @Composable () -> Unit, modifier: Modifier) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    val ext = AppTheme.extended
    val accent = when (val r = ui.result) {
        is PanelScanResult.Access -> if (r.card.allowed) ext.success else ext.danger
        is PanelScanResult.Registration -> if (r.kind == PanelScanResult.Registration.Kind.READY) ext.success else ext.danger
        null -> edu.utcj.acceso.brand.BrandConfig.palette.primaryBright
    }
    Box(modifier.clip(shape).background(Color.Black)) {
        cameraContent()
        ScanFrameOverlay(accent = accent, animate = ui.result == null)
        if (ui.result == null) {
            GuidanceChip(
                if (ui.busy) "Verificando…" else "Coloca el QR dentro del recuadro",
                icon = Icons.Rounded.QrCodeScanner,
                tone = Tone.Info,
                modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.lg)
            )
        }
    }
}

@Composable
private fun ResultPanel(
    ui: ScanUi,
    onRegisterEntry: () -> Unit,
    onDeny: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
    onManualEntry: ((String) -> Unit)?,
    modifier: Modifier
) {
    Column(modifier) {
        AnimatedContent(
            targetState = ui.result,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "scanResult"
        ) { r ->
            when (r) {
                null -> ScanIdleCard()
                is PanelScanResult.Access -> StudentScanCard(
                    card = r.card,
                    awaitingGuard = r.awaitingGuard,
                    onRegisterEntry = onRegisterEntry,
                    onDeny = onDeny,
                    onScanAnother = onDismiss,
                    onManualEntry = onManualEntry
                )
                is PanelScanResult.Registration -> RegistrationReviewCard(
                    result = r,
                    working = ui.working,
                    onApprove = onApprove,
                    onReject = onReject,
                    onClose = onDismiss
                )
            }
        }
    }
}
