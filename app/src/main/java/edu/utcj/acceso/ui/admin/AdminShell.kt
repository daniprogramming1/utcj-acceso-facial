package edu.utcj.acceso.ui.admin

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.data.repository.SyncRepository
import edu.utcj.acceso.data.repository.SyncUiState
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AppListItem
import edu.utcj.acceso.ui.components.AppSnackbarHost
import edu.utcj.acceso.ui.components.BrandLockup
import edu.utcj.acceso.ui.components.ConfirmDialog
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.SyncStatusPill
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.AppWindowSize
import edu.utcj.acceso.util.rememberAppWindowSize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Secciones del panel administrativo. */
enum class AdminSection(
    val label: String,
    val icon: ImageVector,
    val iconOutlined: ImageVector,
    /** Etiqueta corta para la barra inferior del teléfono. */
    val shortLabel: String = label
) {
    DASHBOARD("Inicio", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    STUDENTS("Alumnos", Icons.Rounded.People, Icons.Outlined.People),
    APPROVALS("Aprobaciones", Icons.Rounded.TaskAlt, Icons.Outlined.TaskAlt, shortLabel = "Aprobar"),
    LOG("Bitácora", Icons.AutoMirrored.Rounded.EventNote, Icons.AutoMirrored.Rounded.EventNote),
    VISITORS("Visitantes", Icons.Rounded.Groups, Icons.Rounded.Groups),
    INCIDENTS("Incidencias", Icons.Rounded.ReportProblem, Icons.Rounded.ReportProblem),
    SETTINGS("Configuración", Icons.Rounded.Settings, Icons.Outlined.Settings),
    /** Solo en teléfono: menú con las secciones secundarias. */
    MORE("Más", Icons.Rounded.MoreHoriz, Icons.Rounded.MoreHoriz);

    companion object {
        val phonePrimary = listOf(DASHBOARD, STUDENTS, APPROVALS, LOG, MORE)
        val secondary = listOf(VISITORS, INCIDENTS, SETTINGS)
        val all = listOf(DASHBOARD, STUDENTS, APPROVALS, LOG, VISITORS, INCIDENTS, SETTINGS)
    }
}

/** Insets que cada sección debe aplicar a su contenido (dependen de la navegación usada). */
val LocalSectionInsets = compositionLocalOf { WindowInsets(0, 0, 0, 0) }

/** Acciones globales del panel que las secciones pueden invocar. */
data class AdminActions(
    val onKiosk: () -> Unit = {},
    val onManualEntry: (String?) -> Unit = {},
    val onChangePassword: () -> Unit = {},
    val onEvalMode: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onNavigate: (AdminSection) -> Unit = {}
)

val LocalAdminActions = compositionLocalOf { AdminActions() }

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val auth: AuthRepository,
    students: StudentRepository,
    sync: SyncRepository
) : ViewModel() {
    val guardName: String get() = auth.currentGuardName()
    // Insignia y estado de sincronización son secundarios: si fallan se ocultan, no cierran la app.
    val pendingCount: StateFlow<Int> = flow { emitAll(students.observePending().map { it.size }) }
        .catch { Log.e("AdminViewModel", "Pendientes no disponibles", it); emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val sync: StateFlow<SyncUiState?> = flow<SyncUiState?> { emitAll(sync.observeStatus()) }
        .catch { Log.e("AdminViewModel", "Estado de sincronización no disponible", it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() = auth.logout()
}

@Composable
fun AdminShell(
    onKiosk: () -> Unit,
    onManualEntry: (String?) -> Unit,
    onChangePassword: () -> Unit,
    onEvalMode: () -> Unit,
    onLogout: () -> Unit,
    vm: AdminViewModel = hiltViewModel()
) {
    var section by rememberSaveable { mutableStateOf(AdminSection.DASHBOARD) }
    val pending by vm.pendingCount.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val windowSize = rememberAppWindowSize()

    BackHandler(enabled = section != AdminSection.DASHBOARD) {
        section = if (windowSize == AppWindowSize.Compact && section in AdminSection.secondary) AdminSection.MORE
        else AdminSection.DASHBOARD
    }

    val actions = AdminActions(
        onKiosk = onKiosk,
        onManualEntry = onManualEntry,
        onChangePassword = onChangePassword,
        onEvalMode = onEvalMode,
        onLogout = { confirmLogout = true },
        onNavigate = { section = it }
    )
    AdminShellContent(
        section = section,
        windowSize = windowSize,
        pendingCount = pending,
        guardName = vm.guardName,
        sync = sync,
        actions = actions
    ) { s ->
        when (s) {
            AdminSection.DASHBOARD -> DashboardSection()
            AdminSection.STUDENTS -> StudentsSection()
            AdminSection.APPROVALS -> ApprovalsSection()
            AdminSection.LOG -> AccessLogSection()
            AdminSection.VISITORS -> VisitorsSection()
            AdminSection.INCIDENTS -> IncidentsSection()
            AdminSection.SETTINGS -> SettingsSection()
            AdminSection.MORE -> MoreSection(vm.guardName, sync)
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "¿Cerrar sesión?",
            message = "Tendrás que ingresar la contraseña de guardia para volver al panel.",
            confirmText = "Cerrar sesión",
            icon = Icons.AutoMirrored.Rounded.Logout,
            onDismiss = { confirmLogout = false },
            onConfirm = {
                confirmLogout = false
                vm.logout()
                onLogout()
            }
        )
    }
}

/**
 * Estructura adaptable: barra inferior (teléfono), riel (tableta vertical / plegable) o
 * cajón permanente (tableta horizontal / escritorio).
 */
@Composable
fun AdminShellContent(
    section: AdminSection,
    windowSize: AppWindowSize,
    pendingCount: Int,
    guardName: String,
    sync: SyncUiState?,
    actions: AdminActions,
    sectionContent: @Composable (AdminSection) -> Unit
) {
    val body: @Composable () -> Unit = {
        AnimatedContent(
            targetState = section,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "adminSection"
        ) { s -> sectionContent(s) }
    }
    CompositionLocalProvider(LocalAdminActions provides actions) {
        when (windowSize) {
            AppWindowSize.Compact -> {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar {
                            AdminSection.phonePrimary.forEach { s ->
                                val selected = section == s || (s == AdminSection.MORE && section in AdminSection.secondary)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { actions.onNavigate(s) },
                                    icon = { NavIcon(s, selected, pendingCount) },
                                    label = { Text(s.shortLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) { body() }
                }
            }
            AppWindowSize.Medium -> {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        header = {
                            FloatingActionButton(onClick = actions.onKiosk, modifier = Modifier.padding(top = Spacing.sm)) {
                                Icon(Icons.Rounded.Tv, contentDescription = "Iniciar modo kiosco")
                            }
                        },
                        windowInsets = WindowInsets.safeDrawing
                    ) {
                        Column(
                            Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(Spacing.md))
                            AdminSection.all.forEach { s ->
                                NavigationRailItem(
                                    selected = section == s,
                                    onClick = { actions.onNavigate(s) },
                                    icon = { NavIcon(s, section == s, pendingCount) },
                                    label = { Text(s.label, maxLines = 1) }
                                )
                            }
                            NavigationRailItem(
                                selected = false,
                                onClick = actions.onLogout,
                                icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
                                label = { Text("Salir") }
                            )
                        }
                    }
                    CompositionLocalProvider(LocalSectionInsets provides WindowInsets.navigationBars) {
                        Box(Modifier.weight(1f).fillMaxHeight()) { body() }
                    }
                }
            }
            AppWindowSize.Expanded -> {
                Row(Modifier.fillMaxSize()) {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(288.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        windowInsets = WindowInsets.safeDrawing
                    ) {
                        AdminDrawerContent(section, pendingCount, guardName, sync, actions)
                    }
                    CompositionLocalProvider(LocalSectionInsets provides WindowInsets.navigationBars) {
                        Box(Modifier.weight(1f).fillMaxHeight()) { body() }
                    }
                }
            }
        }
    }
}

@Composable
private fun drawerColors() = NavigationDrawerItemDefaults.colors(
    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
)

@Composable
private fun NavIcon(s: AdminSection, selected: Boolean, pendingCount: Int) {
    val icon = if (selected) s.icon else s.iconOutlined
    if (s == AdminSection.APPROVALS && pendingCount > 0) {
        BadgedBox(badge = { Badge { Text(if (pendingCount > 99) "99+" else pendingCount.toString()) } }) {
            Icon(icon, contentDescription = "${s.label}: $pendingCount pendientes")
        }
    } else {
        Icon(icon, contentDescription = null)
    }
}

@Composable
private fun AdminDrawerContent(
    section: AdminSection,
    pendingCount: Int,
    guardName: String,
    sync: SyncUiState?,
    actions: AdminActions
) {
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.md)) {
        Spacer(Modifier.height(Spacing.xl))
        BrandLockup(Modifier.padding(horizontal = Spacing.md), logoSize = 36.dp, institutionMaxLines = 2)
        Spacer(Modifier.height(Spacing.lg))
        ExtendedFloatingActionButton(
            onClick = actions.onKiosk,
            icon = { Icon(Icons.Rounded.Tv, contentDescription = null) },
            text = { Text("Iniciar modo kiosco") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.lg))
        AdminSection.all.forEach { s ->
            NavigationDrawerItem(
                selected = section == s,
                onClick = { actions.onNavigate(s) },
                icon = { Icon(if (section == s) s.icon else s.iconOutlined, contentDescription = null) },
                label = { Text(s.label) },
                badge = if (s == AdminSection.APPROVALS && pendingCount > 0) {
                    { Badge { Text(pendingCount.toString()) } }
                } else null,
                colors = drawerColors(),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).height(52.dp)
            )
        }
        NavigationDrawerItem(
            selected = false,
            onClick = { actions.onManualEntry(null) },
            icon = { Icon(Icons.Rounded.PersonAddAlt1, contentDescription = null) },
            label = { Text("Entrada manual") },
            colors = drawerColors(),
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).height(52.dp)
        )
        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(Spacing.xl))
        HorizontalDivider()
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.padding(horizontal = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(guardName, size = 40.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(guardName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Guardia en turno", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        SyncStatusPill(sync, Modifier.padding(horizontal = Spacing.md))
        NavigationDrawerItem(
            selected = false,
            onClick = actions.onLogout,
            icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null) },
            label = { Text("Cerrar sesión") },
            colors = drawerColors(),
            modifier = Modifier.padding(vertical = Spacing.sm)
        )
        Spacer(Modifier.height(Spacing.md))
    }
}

/** Menú «Más» del teléfono. */
@Composable
fun MoreSection(guardName: String, sync: SyncUiState?) {
    val actions = LocalAdminActions.current
    SectionScaffold(title = "Más opciones") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenCompact)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(guardName, size = 52.dp)
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(guardName, style = MaterialTheme.typography.titleMedium)
                    Text(BrandConfig.SECURITY_DESK_LABEL, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SyncStatusPill(sync)
            }
            Spacer(Modifier.height(Spacing.xl))
            MoreItem(Icons.Rounded.Tv, Tone.Brand, "Iniciar modo kiosco", "Pantalla de verificación para alumnos", actions.onKiosk)
            MoreItem(Icons.Rounded.PersonAddAlt1, Tone.Info, "Entrada manual", "Registrar un acceso autorizado por ti") { actions.onManualEntry(null) }
            HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
            MoreItem(AdminSection.VISITORS.icon, Tone.Neutral, "Visitantes", "Registro de entradas y salidas") { actions.onNavigate(AdminSection.VISITORS) }
            MoreItem(AdminSection.INCIDENTS.icon, Tone.Warning, "Incidencias", "Reportes de seguridad") { actions.onNavigate(AdminSection.INCIDENTS) }
            MoreItem(AdminSection.SETTINGS.icon, Tone.Neutral, "Configuración", "Reconocimiento, horarios, seguridad y datos") { actions.onNavigate(AdminSection.SETTINGS) }
            HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
            MoreItem(Icons.Rounded.Badge, Tone.Neutral, "Cambiar contraseña", null, actions.onChangePassword)
            MoreItem(Icons.AutoMirrored.Rounded.Logout, Tone.Danger, "Cerrar sesión", null, actions.onLogout)
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun MoreItem(icon: ImageVector, tone: Tone, title: String, subtitle: String?, onClick: () -> Unit) {
    AppListItem(
        title = title,
        subtitle = subtitle,
        leading = { IconBadge(icon, tone) },
        onClick = onClick
    )
}

/**
 * Scaffold común de las secciones: barra superior con logo, acciones, FAB y snackbar, aplicando
 * los insets que corresponden a la navegación activa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionScaffold(
    title: String,
    subtitle: String? = null,
    snackbarHostState: SnackbarHostState? = null,
    floatingActionButton: @Composable () -> Unit = {},
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = { AccesoTopBar(title = title, subtitle = subtitle, actions = actions) },
        floatingActionButton = floatingActionButton,
        snackbarHost = { snackbarHostState?.let { AppSnackbarHost(it) } },
        contentWindowInsets = LocalSectionInsets.current
    ) { padding -> content(padding) }
}

