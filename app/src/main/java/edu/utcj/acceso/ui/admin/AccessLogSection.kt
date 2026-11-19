package edu.utcj.acceso.ui.admin

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.style.TextOverflow
import edu.utcj.acceso.ui.components.PrimaryButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.AccessLogRepository
import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.ui.components.AccessResultChip
import edu.utcj.acceso.ui.components.AppListItem
import edu.utcj.acceso.ui.components.AppSearchField
import edu.utcj.acceso.ui.components.DetailRow
import edu.utcj.acceso.ui.components.EmptyState
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.SkeletonList
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.components.icon
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.components.tone
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Periodo de la bitácora. */
sealed class LogRange(val label: String) {
    data object Today : LogRange("Hoy")
    data object Week : LogRange("7 días")
    data object Month : LogRange("30 días")
    data object All : LogRange("Todo")
    data class Custom(val fromMs: Long, val toMs: Long) :
        LogRange("${TimeUtil.formatShortDate(fromMs)} – ${TimeUtil.formatShortDate(toMs)}")

    /** Límites [desde, hasta] en ms; null = sin límite. */
    fun bounds(nowMs: Long): Pair<Long?, Long?> = when (this) {
        Today -> TimeUtil.startOfDayMs(nowMs) to null
        Week -> TimeUtil.daysAgoStartMs(6, nowMs) to null
        Month -> TimeUtil.daysAgoStartMs(29, nowMs) to null
        All -> null to null
        is Custom -> TimeUtil.startOfDayMs(fromMs) to TimeUtil.endOfDayMs(toMs)
    }

    /** Texto para el encabezado del PDF. */
    fun reportLabel(nowMs: Long): String = when (this) {
        Today -> "Hoy, ${TimeUtil.formatShortDate(nowMs)}"
        Week -> "Últimos 7 días (al ${TimeUtil.formatShortDate(nowMs)})"
        Month -> "Últimos 30 días (al ${TimeUtil.formatShortDate(nowMs)})"
        All -> "Todos los registros"
        is Custom -> label
    }

    companion object {
        // Getter (no propiedad): evita valores null por el orden de inicialización de objetos.
        val presets: List<LogRange> get() = listOf(Today, Week, Month, All)
    }
}

/** Filtro por resultado. */
enum class LogResultFilter(val label: String, val result: AccessResult?) {
    ALL("Todos", null), ALLOWED("Permitidos", AccessResult.ALLOWED), DENIED("Denegados", AccessResult.DENIED),
    QR("QR", AccessResult.QR), MANUAL("Manual", AccessResult.MANUAL)
}

/** Filtro de texto puro sobre la bitácora (nombre, matrícula, motivo, guardia). */
fun filterEvents(events: List<AccessEvent>, query: String): List<AccessEvent> {
    val q = normalizeSearch(query)
    if (q.isEmpty()) return events
    return events.filter { e ->
        normalizeSearch(e.matricula).contains(q) || normalizeSearch(e.nombre).contains(q) ||
            normalizeSearch(e.reason.orEmpty()).contains(q) || normalizeSearch(e.authorizingGuard.orEmpty()).contains(q)
    }
}

/**
 * Fila de la bitácora: un evento, o varios rechazos seguidos del mismo alumno con el mismo motivo
 * (p. ej. alguien que insiste con un QR vencido). [latest] es el más reciente.
 */
data class LogEntry(val latest: AccessEvent, val count: Int = 1, val earliestMs: Long = latest.datetimeMs)

/**
 * Junta rechazos consecutivos (lista ordenada desc.) del mismo alumno con el mismo motivo para que
 * los intentos repetidos no empujen fuera de la pantalla al resto de la fila. Los permitidos nunca
 * se agrupan: cada entrada cuenta.
 */
fun groupConsecutiveDenials(events: List<AccessEvent>): List<LogEntry> {
    val out = mutableListOf<LogEntry>()
    for (e in events) {
        val last = out.lastOrNull()?.latest
        val sameRun = last != null &&
            e.result == AccessResult.DENIED && last.result == AccessResult.DENIED &&
            e.matricula == last.matricula && e.reason == last.reason
        if (sameRun) {
            val g = out.last()
            out[out.lastIndex] = g.copy(count = g.count + 1, earliestMs = e.datetimeMs)
        } else {
            out += LogEntry(e)
        }
    }
    return out
}

/** Agrupa eventos (ya ordenados desc.) por día: «Hoy», «Ayer», «lun 5 oct»… */
fun groupByDay(events: List<AccessEvent>, nowMs: Long): List<Pair<String, List<AccessEvent>>> =
    events.groupBy { TimeUtil.startOfDayMs(it.datetimeMs) }
        .toSortedMap(compareByDescending { it })
        .map { (day, list) -> TimeUtil.dayHeader(day, nowMs) to list }

data class AccessLogUi(
    val loading: Boolean = true,
    val query: String = "",
    val range: LogRange = LogRange.Today,
    val result: LogResultFilter = LogResultFilter.ALL,
    val events: List<AccessEvent> = emptyList(),
    val nowMs: Long = System.currentTimeMillis()
) {
    val allowedCount: Int get() = events.count { it.result != AccessResult.DENIED }
    val deniedCount: Int get() = events.count { it.result == AccessResult.DENIED }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccessLogViewModel @Inject constructor(
    private val repo: AccessLogRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val range = MutableStateFlow<LogRange>(LogRange.Today)
    private val result = MutableStateFlow(LogResultFilter.ALL)

    val ui: StateFlow<AccessLogUi> = combine(range, result) { r, f -> r to f }
        .flatMapLatest { (r, f) ->
            val now = System.currentTimeMillis()
            val (from, to) = r.bounds(now)
            repo.observeFiltered(from, to, null, f.result).map { Triple(r, f, it) }
        }
        .combine(query) { (r, f, events), q ->
            AccessLogUi(false, q, r, f, filterEvents(events, q), System.currentTimeMillis())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccessLogUi())

    fun setQuery(q: String) { query.value = q }
    fun setRange(r: LogRange) { range.value = r }
    fun setResult(r: LogResultFilter) { result.value = r }

    suspend fun exportCsv(): File = repo.exportCsv(ui.value.events)
    suspend fun exportPdf(): File = repo.exportPdf(ui.value.events, ui.value.range.reportLabel(System.currentTimeMillis()))
}

private fun share(context: Context, file: File, mime: String, title: String) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, title))
}

@Composable
fun AccessLogSection(vm: AccessLogViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    AccessLogContent(
        ui = ui,
        snackbar = snackbar,
        onQuery = vm::setQuery,
        onRange = vm::setRange,
        onResult = vm::setResult,
        onExportCsv = {
            scope.launch {
                runCatching { vm.exportCsv() }
                    .onSuccess { share(context, it, "text/csv", "Bitácora de accesos (CSV)") }
                    .onFailure { snackbar.showSnackbar("No se pudo exportar: ${it.message}") }
            }
        },
        onExportPdf = {
            scope.launch {
                runCatching { vm.exportPdf() }
                    .onSuccess { share(context, it, "application/pdf", "Reporte de accesos (PDF)") }
                    .onFailure { snackbar.showSnackbar("No se pudo exportar: ${it.message}") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessLogContent(
    ui: AccessLogUi,
    snackbar: SnackbarHostState? = null,
    onQuery: (String) -> Unit,
    onRange: (LogRange) -> Unit,
    onResult: (LogResultFilter) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<AccessEvent?>(null) }
    SectionScaffold(
        title = "Bitácora",
        subtitle = "Solo lectura · ${ui.events.size} registros",
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = { menu = true }, enabled = ui.events.isNotEmpty()) {
                Icon(Icons.Rounded.Download, contentDescription = "Exportar bitácora")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Exportar CSV (Excel)") },
                    leadingIcon = { Icon(Icons.Rounded.TableChart, contentDescription = null) },
                    onClick = { menu = false; onExportCsv() }
                )
                DropdownMenuItem(
                    text = { Text("Reporte PDF") },
                    leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                    onClick = { menu = false; onExportPdf() }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = Spacing.screenCompact)) {
                AppSearchField(ui.query, onQuery, "Buscar nombre o matrícula", Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(Spacing.sm))
            // Un solo control con el resumen de los filtros activos (antes: dos filas de chips cortadas).
            FilledTonalButton(
                onClick = { filters = true },
                modifier = Modifier.padding(horizontal = Spacing.screenCompact).fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Filtros · ${ui.range.label} · ${ui.result.label}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(Modifier.padding(horizontal = Spacing.screenCompact), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusPill("${ui.allowedCount} permitidos", Tone.Success)
                StatusPill("${ui.deniedCount} denegados", Tone.Danger)
            }
            Spacer(Modifier.height(Spacing.sm))
            when {
                ui.loading -> SkeletonList(modifier = Modifier.padding(horizontal = Spacing.screenCompact))
                ui.events.isEmpty() -> EmptyState(
                    if (ui.query.isBlank()) "Nadie ha cruzado" else "Sin coincidencias",
                    if (ui.query.isBlank()) "La caseta está tranquila en este periodo. Prueba con «7 días» o «Todo» en Filtros."
                    else "Nadie con «${ui.query}» en este periodo. Revisa la matrícula o amplía el periodo.",
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                    groupByDay(ui.events, ui.nowMs).forEach { (header, list) ->
                        stickyHeaderCompat(header)
                        items(groupConsecutiveDenials(list), key = { it.latest.id }) { entry ->
                            val e = entry.latest
                            AppListItem(
                                title = e.nombre,
                                subtitle = "${e.matricula} · ${e.method.labelEs()}",
                                // El motivo es lo importante de un rechazo: va en su propia línea.
                                supporting = when {
                                    entry.count > 1 -> "${entry.count} intentos · ${e.reason ?: "Denegado"}"
                                    else -> e.reason
                                },
                                leading = { IconBadge(e.method.icon(), e.result.tone()) },
                                trailing = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        AccessResultChip(e.result)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            if (entry.count > 1) "${TimeUtil.formatTime(entry.earliestMs)}–${TimeUtil.formatTime(e.datetimeMs)}"
                                            else TimeUtil.formatTime(e.datetimeMs),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { detail = e },
                                modifier = Modifier.padding(horizontal = Spacing.sm)
                            )
                        }
                    }
                }
            }
        }
    }

    if (filters) {
        ModalBottomSheet(onDismissRequest = { filters = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding().padding(bottom = Spacing.xl)) {
                Text("Filtros", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Spacing.lg))
                Text("Periodo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.sm))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    LogRange.presets.forEach { r -> SheetChip(r.label, ui.range == r) { onRange(r) } }
                    SheetChip((ui.range as? LogRange.Custom)?.label ?: "Personalizado…", ui.range is LogRange.Custom) {
                        filters = false
                        picker = true
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                Text("Resultado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.sm))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    LogResultFilter.entries.forEach { f -> SheetChip(f.label, ui.result == f) { onResult(f) } }
                }
                Spacer(Modifier.height(Spacing.xl))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onRange(LogRange.Today); onResult(LogResultFilter.ALL) }) { Text("Restablecer") }
                    Spacer(Modifier.weight(1f))
                    PrimaryButton("Ver ${ui.events.size} registros", onClick = { filters = false })
                }
            }
        }
    }

    if (picker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { picker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        // El selector entrega medianoche UTC; se convierte a la zona del plantel.
                        val off = { utc: Long -> utc - TimeUtil.zone.getOffset(utc) }
                        val s = state.selectedStartDateMillis!!
                        val e = state.selectedEndDateMillis ?: s
                        onRange(LogRange.Custom(off(s) + 12 * 3_600_000L, off(e) + 12 * 3_600_000L))
                        picker = false
                    }
                ) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { picker = false }) { Text("Cancelar") } }
        ) {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = { Text("Selecciona el periodo", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
            )
        }
    }

    detail?.let { e ->
        ModalBottomSheet(onDismissRequest = { detail = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.padding(horizontal = Spacing.xl).navigationBarsPadding().padding(bottom = Spacing.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(e.method.icon(), e.result.tone(), size = 52.dp)
                    Spacer(Modifier.width(Spacing.lg))
                    Column(Modifier.weight(1f)) {
                        Text(e.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(TimeUtil.formatDateTime(e.datetimeMs), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AccessResultChip(e.result)
                }
                Spacer(Modifier.height(Spacing.lg))
                DetailRow("Matrícula", e.matricula, icon = Icons.Rounded.Badge)
                DetailRow("Método", e.method.labelEs(), icon = e.method.icon())
                e.reason?.let { DetailRow("Motivo", it, icon = Icons.Rounded.Info) }
                e.authorizingGuard?.let { DetailRow("Autorizó", it, icon = Icons.Rounded.Badge) }
                e.similarity?.let { DetailRow("Similitud", "%.2f".format(it), icon = Icons.Rounded.Percent) }
                e.verifyDurationMs?.let { DetailRow("Tiempo de verificación", formatVerifyMs(it), icon = Icons.Rounded.Timer) }
                DetailRow("Sincronizado", if (e.synced) "Sí" else "Pendiente", icon = Icons.Rounded.Schedule)
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Los registros de la bitácora no se pueden editar ni borrar.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SheetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null
    )
}

/** Encabezado de día dentro de la lista. */
private fun androidx.compose.foundation.lazy.LazyListScope.stickyHeaderCompat(title: String) {
    item(key = "h_$title") {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.screenCompact, vertical = Spacing.sm)
                .semantics { heading() }
        )
    }
}
