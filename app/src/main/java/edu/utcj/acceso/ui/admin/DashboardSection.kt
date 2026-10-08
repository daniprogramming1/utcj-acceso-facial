package edu.utcj.acceso.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.AccessLogRepository
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.analytics.AccessAlert
import edu.utcj.acceso.domain.analytics.AlertType
import edu.utcj.acceso.domain.analytics.AlertsEngine
import edu.utcj.acceso.domain.analytics.DashboardCalculator
import edu.utcj.acceso.domain.analytics.DashboardStats
import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.ui.components.AccessResultChip
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.AppListItem
import edu.utcj.acceso.ui.components.BarChart
import edu.utcj.acceso.ui.components.ChartLegendItem
import edu.utcj.acceso.ui.components.DonutChart
import edu.utcj.acceso.ui.components.DonutSegment
import edu.utcj.acceso.ui.components.EmptyState
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.KpiCard
import edu.utcj.acceso.ui.components.KpiTrend
import edu.utcj.acceso.ui.components.LineChart
import edu.utcj.acceso.ui.components.SectionHeader
import edu.utcj.acceso.ui.components.SkeletonBlock
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUi(
    val loading: Boolean = true,
    val stats: DashboardStats = DashboardStats(),
    val alerts: List<AccessAlert> = emptyList(),
    val pendingCount: Int = 0,
    val nowMs: Long = System.currentTimeMillis()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    log: AccessLogRepository,
    students: StudentRepository,
    private val settings: SettingsRepository,
    private val auth: AuthRepository
) : ViewModel() {
    val guardName: String get() = auth.currentGuardName()

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val ui: StateFlow<DashboardUi> = combine(
        log.observeSince(TimeUtil.daysAgoStartMs(7)),
        students.observePending(),
        ticker
    ) { events, pending, now ->
        val todayStart = TimeUtil.startOfDayMs(now)
        DashboardUi(
            loading = false,
            stats = DashboardCalculator.compute(events, now),
            alerts = AlertsEngine.build(
                events.filter { it.datetimeMs >= todayStart }, settings.getHoursStart(), settings.getHoursEnd()
            ),
            pendingCount = pending.size,
            nowMs = now
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUi())
}

@Composable
fun DashboardSection(vm: DashboardViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val actions = LocalAdminActions.current
    DashboardContent(
        ui = ui,
        guardName = vm.guardName,
        onKiosk = actions.onKiosk,
        onManualEntry = { actions.onManualEntry(null) },
        onOpen = actions.onNavigate
    )
}

/** 850 → «0.85 s»; 1 500 → «1.5 s». */
internal fun formatVerifyMs(ms: Long): String =
    if (ms < 1000) "%.2f s".format(ms / 1000f) else "%.1f s".format(ms / 1000f)

private fun greeting(nowMs: Long): String = when (TimeUtil.hourOfDay(nowMs)) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardContent(
    ui: DashboardUi,
    guardName: String,
    onKiosk: () -> Unit,
    onManualEntry: () -> Unit,
    onOpen: (AdminSection) -> Unit
) {
    SectionScaffold(title = "Inicio", subtitle = TimeUtil.formatLongDate(ui.nowMs).replaceFirstChar { it.uppercase() }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 720.dp
            val hPad = if (wide) Spacing.screenExpanded else Spacing.screenCompact
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = hPad, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                HeroCard(guardName, ui, onKiosk, onManualEntry)

                ui.alerts.take(3).forEach { a ->
                    AlertBanner(
                        title = a.title,
                        message = a.message,
                        tone = if (a.type == AlertType.CONSECUTIVE_FAILURES) Tone.Danger else Tone.Warning,
                        icon = if (a.type == AlertType.CONSECUTIVE_FAILURES) Icons.Rounded.WarningAmber else Icons.Rounded.NightsStay,
                        action = { TextButton(onClick = { onOpen(AdminSection.LOG) }) { Text("Ver") } }
                    )
                }

                KpiGrid(ui, wide, onOpen)

                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        HourlyCard(ui, Modifier.weight(1.6f))
                        DonutCard(ui, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        WeekCard(ui, Modifier.weight(1.6f))
                        RecentCard(ui.stats.recent, ui.nowMs, Modifier.weight(1f)) { onOpen(AdminSection.LOG) }
                    }
                } else {
                    HourlyCard(ui, Modifier.fillMaxWidth())
                    DonutCard(ui, Modifier.fillMaxWidth())
                    WeekCard(ui, Modifier.fillMaxWidth())
                    RecentCard(ui.stats.recent, ui.nowMs, Modifier.fillMaxWidth()) { onOpen(AdminSection.LOG) }
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun HeroCard(guardName: String, ui: DashboardUi, onKiosk: () -> Unit, onManualEntry: () -> Unit) {
    val ext = AppTheme.extended
    Surface(shape = AppShapes.card, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .background(Brush.linearGradient(listOf(ext.heroGradientStart, ext.heroGradientEnd)))
                .padding(Spacing.xl)
        ) {
            Column {
                Text("${greeting(ui.nowMs)}, ${guardName.substringBefore(' ').ifBlank { guardName }}",
                    style = MaterialTheme.typography.headlineSmall, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Spacing.xs))
                val s = ui.stats
                Text(
                    if (s.attemptsToday == 0) "Aún no hay accesos registrados hoy."
                    else "Hoy van ${s.entriesToday} entradas y ${s.deniedToday} intentos denegados.",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(Spacing.lg))
                BoxWithConstraints {
                    // En pantallas angostas se ocultan los íconos para que el texto quepa en una línea.
                    val icons = maxWidth >= 340.dp
                    val pad = androidx.compose.foundation.layout.PaddingValues(horizontal = if (icons) 20.dp else 12.dp, vertical = 10.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Button(
                            onClick = onKiosk,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ext.heroGradientEnd),
                            shape = AppShapes.button,
                            contentPadding = pad
                        ) {
                            if (icons) {
                                Icon(Icons.Rounded.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Spacing.sm))
                            }
                            Text("Modo kiosco", maxLines = 1)
                        }
                        Button(
                            onClick = onManualEntry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                            shape = AppShapes.button,
                            contentPadding = pad
                        ) {
                            if (icons) {
                                Icon(Icons.Rounded.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(Spacing.sm))
                            }
                            Text("Entrada manual", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KpiGrid(ui: DashboardUi, wide: Boolean, onOpen: (AdminSection) -> Unit) {
    val s = ui.stats
    val delta = s.deltaVsYesterdayPct
    val items: List<@Composable (Modifier) -> Unit> = listOf(
        { m ->
            KpiCard(
                "Entradas hoy", if (ui.loading) "—" else s.entriesToday.toString(), Icons.Rounded.DoorFront, Tone.Brand, m,
                trend = delta?.let { KpiTrend("${if (it >= 0) "+" else ""}$it %", it >= 0) },
                supporting = if (delta != null) "vs ayer" else "Ayer: ${s.entriesYesterday}",
                onClick = { onOpen(AdminSection.LOG) }
            )
        },
        { m ->
            KpiCard(
                "Tasa de éxito", if (s.attemptsToday == 0) "—" else "%.0f %%".format(s.successRate), Icons.Rounded.Verified, Tone.Success, m,
                supporting = "${s.attemptsToday} intentos"
            )
        },
        { m ->
            KpiCard(
                "Tiempo promedio", if (s.avgVerifyMs <= 0) "—" else formatVerifyMs(s.avgVerifyMs), Icons.Rounded.Timer, Tone.Info, m,
                supporting = "por verificación"
            )
        },
        { m ->
            KpiCard(
                "Pendientes", ui.pendingCount.toString(), Icons.Rounded.HourglassTop,
                if (ui.pendingCount > 0) Tone.Warning else Tone.Neutral, m,
                supporting = if (ui.pendingCount > 0) "Por aprobar" else "Todo al día",
                onClick = { onOpen(AdminSection.APPROVALS) }
            )
        }
    )
    if (wide) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            items.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            items.chunked(2).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    row.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun HourlyCard(ui: DashboardUi, modifier: Modifier) {
    val s = ui.stats
    AppCard(modifier) {
        SectionHeader(
            "Accesos por hora",
            subtitle = s.peakHour?.let { "Hora pico: %02d:00".format(it) } ?: "Hoy",
            action = { IconBadge(Icons.Rounded.Bolt, Tone.Brand, size = 32.dp) }
        )
        Spacer(Modifier.height(Spacing.lg))
        // Muestra de 6:00 a 22:00 para que las barras sean legibles.
        val from = 6
        val to = 22
        BarChart(
            values = s.byHour.subList(from, to + 1).map { it.toFloat() },
            labels = (from..to).map { "%02d".format(it) },
            highlightIndex = s.peakHour?.takeIf { it in from..to }?.minus(from),
            labelEvery = 4,
            modifier = Modifier.fillMaxWidth().height(168.dp),
            description = "Accesos por hora de hoy"
        )
    }
}

@Composable
private fun DonutCard(ui: DashboardUi, modifier: Modifier) {
    val s = ui.stats
    val ext = AppTheme.extended
    AppCard(modifier) {
        SectionHeader("Resultado de verificaciones", subtitle = "Hoy")
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                segments = listOf(
                    DonutSegment(s.entriesToday.toFloat(), ext.success, "Permitidos"),
                    DonutSegment(s.deniedToday.toFloat(), ext.danger, "Denegados")
                ),
                centerValue = if (s.attemptsToday == 0) "—" else "%.0f%%".format(s.successRate),
                centerLabel = "éxito",
                modifier = Modifier.size(132.dp)
            )
            Spacer(Modifier.width(Spacing.xl))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ChartLegendItem(ext.success, "Permitidos", s.entriesToday.toString())
                ChartLegendItem(ext.danger, "Denegados", s.deniedToday.toString())
            }
        }
    }
}

@Composable
private fun WeekCard(ui: DashboardUi, modifier: Modifier) {
    val days = ui.stats.last7Days
    AppCard(modifier) {
        SectionHeader("Últimos 7 días", subtitle = "Entradas permitidas por día")
        Spacer(Modifier.height(Spacing.lg))
        if (days.isEmpty()) {
            SkeletonBlock(Modifier.fillMaxWidth(), height = 140.dp)
        } else {
            LineChart(
                values = days.map { it.allowed.toFloat() },
                labels = days.map { TimeUtil.weekdayShort(it.dayStartMs).replaceFirstChar { c -> c.uppercase() } },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                description = "Tendencia de entradas de los últimos 7 días"
            )
        }
    }
}

@Composable
private fun RecentCard(recent: List<AccessEvent>, nowMs: Long, modifier: Modifier, onSeeAll: () -> Unit) {
    AppCard(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.lg)) {
        SectionHeader(
            "Actividad reciente",
            modifier = Modifier.padding(horizontal = Spacing.lg),
            action = { TextButton(onClick = onSeeAll) { Text("Ver todo") } }
        )
        if (recent.isEmpty()) {
            EmptyState(
                "Sin actividad",
                "Los accesos aparecerán aquí en tiempo real.",
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg)
            )
        } else {
            recent.take(6).forEach { e ->
                AppListItem(
                    title = e.nombre,
                    subtitle = "${e.matricula} · ${e.method.labelEs()}",
                    leading = { InitialsAvatar(e.nombre, size = 40.dp) },
                    trailing = {
                        Column(horizontalAlignment = Alignment.End) {
                            AccessResultChip(e.result)
                            Spacer(Modifier.height(2.dp))
                            Text(TimeUtil.relative(e.datetimeMs, nowMs), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }
    }
}

