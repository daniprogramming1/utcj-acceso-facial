package edu.utcj.acceso.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.BuildConfigProxy
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.data.biometric.FaceMatcher
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.data.repository.StudentStatusRepository
import edu.utcj.acceso.data.repository.SyncRepository
import edu.utcj.acceso.data.repository.SyncUiState
import edu.utcj.acceso.domain.model.ThemeMode
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.BrandLogoTile
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.SyncStatusPill
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Valores de configuración mostrados en pantalla. */
data class SettingsUi(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val threshold: Float = FaceMatcher.DEFAULT_THRESHOLD,
    val liveness: Boolean = false,
    val legacyFace: Boolean = false,
    val hoursStart: Int = SettingsRepository.DEFAULT_HOURS_START,
    val hoursEnd: Int = SettingsRepository.DEFAULT_HOURS_END,
    val kioskOrientation: SettingsRepository.KioskOrientation = SettingsRepository.KioskOrientation.LANDSCAPE,
    val kioskIdleMs: Long = SettingsRepository.DEFAULT_KIOSK_IDLE_MS,
    val kioskSound: Boolean = true
)

/** Explicación en lenguaje sencillo del umbral elegido. */
fun thresholdExplanation(t: Float): String = when {
    t < FaceMatcher.DEFAULT_THRESHOLD -> "Permisivo: reconoce más rápido pero aumenta el riesgo de confundir a dos personas."
    t < 0.70f -> "Equilibrado (recomendado): buen balance entre seguridad y rapidez."
    t < 0.82f -> "Estricto: menos falsos aceptados; algunos alumnos tendrán que intentar dos veces."
    else -> "Muy estricto: máxima seguridad; espera más rechazos con poca luz o lentes."
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val students: StudentRepository,
    private val statusRepo: StudentStatusRepository,
    private val syncRepo: SyncRepository
) : ViewModel() {
    val sync: StateFlow<SyncUiState?> = syncRepo.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun load() = SettingsUi(
        themeMode = settings.themeModeFlow.value,
        threshold = settings.getFaceThreshold(),
        liveness = settings.isLivenessEnabled(),
        legacyFace = settings.useLegacyFaceEmbedding(),
        hoursStart = settings.getHoursStart(),
        hoursEnd = settings.getHoursEnd(),
        kioskOrientation = settings.getKioskOrientation(),
        kioskIdleMs = settings.getKioskIdleMs(),
        kioskSound = settings.isKioskSoundEnabled()
    )

    fun apply(ui: SettingsUi) {
        settings.setThemeMode(ui.themeMode)
        settings.setFaceThreshold(ui.threshold)
        settings.setLivenessEnabled(ui.liveness)
        settings.setUseLegacyFaceEmbedding(ui.legacyFace)
        settings.setHours(ui.hoursStart, ui.hoursEnd)
        settings.setKioskOrientation(ui.kioskOrientation)
        settings.setKioskIdleMs(ui.kioskIdleMs)
        settings.setKioskSoundEnabled(ui.kioskSound)
    }

    suspend fun importCsv(input: java.io.InputStream) = students.importStatusFromStream(input)
    suspend fun loadAssets() = statusRepo.refreshFromInstitutionalSource()
    fun syncNow() = syncRepo.triggerSync()
}

@Composable
fun SettingsSection(vm: SettingsViewModel = hiltViewModel()) {
    var ui by remember { mutableStateOf(vm.load()) }
    val sync by vm.sync.collectAsStateWithLifecycle()
    val actions = LocalAdminActions.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val r = runCatching { context.contentResolver.openInputStream(uri)?.use { vm.importCsv(it) } }
                snackbar.showSnackbar(if (r.isSuccess) "Estatus importados desde CSV" else "No se pudo leer el archivo")
            }
        }
    }
    SettingsContent(
        ui = ui,
        sync = sync,
        snackbar = snackbar,
        onChange = { new ->
            val engineChanged = new.legacyFace != ui.legacyFace
            ui = new
            vm.apply(new)
            if (engineChanged) scope.launch { snackbar.showSnackbar("Reinicia la app para aplicar el cambio de motor facial") }
        },
        onImportCsv = { picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values")) },
        onLoadSample = { scope.launch { vm.loadAssets(); snackbar.showSnackbar("CSV de ejemplo cargado") } },
        onSyncNow = { vm.syncNow(); scope.launch { snackbar.showSnackbar("Sincronización programada") } },
        onChangePassword = actions.onChangePassword,
        onLogout = actions.onLogout,
        onEvalMode = actions.onEvalMode,
        onKiosk = actions.onKiosk
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    ui: SettingsUi,
    sync: SyncUiState?,
    snackbar: SnackbarHostState? = null,
    onChange: (SettingsUi) -> Unit,
    onImportCsv: () -> Unit,
    onLoadSample: () -> Unit,
    onSyncNow: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onEvalMode: () -> Unit,
    onKiosk: () -> Unit
) {
    var versionTaps by remember { mutableIntStateOf(0) }
    SectionScaffold(title = "Configuración", snackbarHostState = snackbar) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val twoCol = maxWidth >= 840.dp
            val hPad = if (maxWidth >= 600.dp) Spacing.screenExpanded else Spacing.screenCompact
            val left: @Composable ColumnScope.() -> Unit = {
                AppearanceCard(ui, onChange)
                RecognitionCard(ui, onChange)
                KioskCard(ui, onChange, onKiosk)
                HoursCard(ui, onChange)
            }
            val right: @Composable ColumnScope.() -> Unit = {
                SecurityCard(onChangePassword, onLogout)
                DataCard(sync, onImportCsv, onLoadSample, onSyncNow)
                AboutCard(onVersionTap = {
                    versionTaps++
                    if (versionTaps >= 7) { versionTaps = 0; onEvalMode() }
                })
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = hPad, vertical = Spacing.sm)
            ) {
                if (twoCol) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.lg), content = left)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.lg), content = right)
                    }
                } else {
                    Column(
                        Modifier.widthIn(max = Spacing.contentMaxWidth).align(Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                    ) { left(); right() }
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, icon: ImageVector, tone: Tone, content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tone, size = 36.dp)
            Spacer(Modifier.width(Spacing.md))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.md))
        content()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, icon: ImageVector? = null, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String?, icon: ImageVector, onClick: () -> Unit, danger: Boolean = false) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) color else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceCard(ui: SettingsUi, onChange: (SettingsUi) -> Unit) {
    SettingsGroup("Apariencia", Icons.Rounded.Contrast, Tone.Brand) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = ui.themeMode == m,
                    onClick = { onChange(ui.copy(themeMode = m)) },
                    shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size)
                ) { Text(m.labelEs) }
            }
        }
    }
}

@Composable
private fun RecognitionCard(ui: SettingsUi, onChange: (SettingsUi) -> Unit) {
    var t by remember(ui.threshold) { mutableFloatStateOf(ui.threshold) }
    SettingsGroup("Reconocimiento facial", Icons.Rounded.Face, Tone.Info) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Umbral de coincidencia", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("%.2f".format(t), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = t,
            onValueChange = { t = (Math.round(it * 100) / 100f) },
            onValueChangeFinished = { onChange(ui.copy(threshold = t)) },
            valueRange = 0.50f..0.95f,
            modifier = Modifier.semantics {
                contentDescription = "Umbral de coincidencia facial"
                stateDescription = "%.2f".format(t)
            }
        )
        Text(thresholdExplanation(t), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Recomendado: %.2f (MobileFaceNet) · motor legado ≈ %.2f".format(FaceMatcher.DEFAULT_THRESHOLD, FaceMatcher.LEGACY_THRESHOLD),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (t < FaceMatcher.DEFAULT_THRESHOLD) {
            Spacer(Modifier.height(Spacing.sm))
            AlertBanner("Umbral por debajo del recomendado", Tone.Warning, Icons.Rounded.WarningAmber,
                message = "Aumenta el riesgo de que una persona sea reconocida como otra.")
        }
        HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
        SwitchRow("Prueba de vida", "Pide parpadear o girar la cabeza antes de verificar", ui.liveness, Icons.Rounded.RemoveRedEye) {
            onChange(ui.copy(liveness = it))
        }
        SwitchRow("Motor legado (histograma)", "Solo para pruebas. Requiere reiniciar la app.", ui.legacyFace, Icons.Rounded.Memory) {
            onChange(ui.copy(legacyFace = it))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KioskCard(ui: SettingsUi, onChange: (SettingsUi) -> Unit, onKiosk: () -> Unit) {
    SettingsGroup("Modo kiosco", Icons.Rounded.Tv, Tone.Brand) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.md))
            Text("Orientación", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(Spacing.sm))
        val orientations = SettingsRepository.KioskOrientation.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            orientations.forEachIndexed { i, o ->
                SegmentedButton(
                    selected = ui.kioskOrientation == o,
                    onClick = { onChange(ui.copy(kioskOrientation = o)) },
                    shape = SegmentedButtonDefaults.itemShape(i, orientations.size)
                ) { Text(o.labelEs, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.md))
            Text("Regresar a la cámara tras un resultado", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(Spacing.sm))
        val opts = SettingsRepository.KIOSK_IDLE_OPTIONS_MS
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            opts.forEachIndexed { i, ms ->
                SegmentedButton(
                    selected = ui.kioskIdleMs == ms,
                    onClick = { onChange(ui.copy(kioskIdleMs = ms)) },
                    shape = SegmentedButtonDefaults.itemShape(i, opts.size)
                ) { Text("${ms / 1000} s") }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        SwitchRow("Sonido de confirmación", "Tono breve al permitir o denegar", ui.kioskSound, Icons.AutoMirrored.Rounded.VolumeUp) {
            onChange(ui.copy(kioskSound = it))
        }
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton("Iniciar modo kiosco", onKiosk, Modifier.fillMaxWidth(), icon = Icons.Rounded.Tv)
    }
}

@Composable
private fun HoursCard(ui: SettingsUi, onChange: (SettingsUi) -> Unit) {
    var range by remember(ui.hoursStart, ui.hoursEnd) { mutableStateOf(ui.hoursStart.toFloat()..ui.hoursEnd.toFloat()) }
    SettingsGroup("Horario de acceso", Icons.Rounded.Schedule, Tone.Warning) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Permitir entradas de", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                "%02d:00 – %02d:00".format(range.start.toInt(), range.endInclusive.toInt()),
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary
            )
        }
        RangeSlider(
            value = range,
            onValueChange = { range = it.start.toInt().toFloat()..it.endInclusive.toInt().toFloat() },
            onValueChangeFinished = { onChange(ui.copy(hoursStart = range.start.toInt(), hoursEnd = range.endInclusive.toInt())) },
            valueRange = 0f..23f,
            steps = 22
        )
        Text(
            "Fuera de este horario el kiosco deniega el acceso y el panel muestra una alerta.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecurityCard(onChangePassword: () -> Unit, onLogout: () -> Unit) {
    SettingsGroup("Seguridad", Icons.Rounded.Key, Tone.Danger) {
        ActionRow("Cambiar contraseña", "Contraseña del personal de guardia", Icons.Rounded.Key, onChangePassword)
        HorizontalDivider()
        ActionRow("Cerrar sesión", null, Icons.AutoMirrored.Rounded.Logout, onLogout, danger = true)
    }
}

@Composable
private fun DataCard(sync: SyncUiState?, onImportCsv: () -> Unit, onLoadSample: () -> Unit, onSyncNow: () -> Unit) {
    SettingsGroup("Datos y sincronización", Icons.Rounded.CloudSync, Tone.Success) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Estado", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            SyncStatusPill(sync)
        }
        ActionRow("Sincronizar ahora", "Envía la bitácora pendiente al servidor institucional", Icons.Rounded.CloudSync, onSyncNow)
        HorizontalDivider()
        ActionRow("Importar estatus (CSV)", "Columnas: matricula, nombre, estatus", Icons.Rounded.UploadFile, onImportCsv)
        HorizontalDivider()
        ActionRow("Cargar CSV de ejemplo", "Datos de demostración incluidos en la app", Icons.Rounded.Inventory2, onLoadSample)
    }
}

@Composable
private fun AboutCard(onVersionTap: () -> Unit) {
    SettingsGroup("Acerca de", Icons.Rounded.Info, Tone.Neutral) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLogoTile(size = 48.dp)
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(BrandConfig.APP_NAME, style = MaterialTheme.typography.titleMedium)
                Text(BrandConfig.INSTITUTION_NAME, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Versión ${BuildConfigProxy.VERSION_NAME} (${BuildConfigProxy.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onVersionTap).padding(vertical = Spacing.xs)
        )
        Text("Motor: MobileFaceNet (TFLite) + ML Kit · 100 % en el dispositivo", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Soporte: ${BrandConfig.SUPPORT_EMAIL}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Sin fotografías · Vectores biométricos cifrados (AES-GCM, Android Keystore)", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
