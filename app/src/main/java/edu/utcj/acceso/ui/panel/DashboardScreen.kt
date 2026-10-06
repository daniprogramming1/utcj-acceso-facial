package edu.utcj.acceso.ui.panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.theme.AccessGreen
import edu.utcj.acceso.ui.theme.AccessRed
import edu.utcj.acceso.ui.theme.UtcjBlue
import edu.utcj.acceso.util.TimeUtil

@Composable
fun DashboardScreen(onBack: () -> Unit, vm: DashboardViewModel = hiltViewModel()) {
    var stats by remember { mutableStateOf(DashboardViewModel.Stats()) }
    LaunchedEffect(Unit) { stats = vm.compute() }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Tablero", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            Text("Entradas hoy: ${stats.totalToday}")
            Text("Éxito: ${"%.1f".format(stats.successPct)}% · Fallo: ${"%.1f".format(stats.failPct)}%")
            Text("Tiempo medio verificación: ${stats.avgMs} ms")
            Spacer(Modifier.height(16.dp))
            Text("Por hora (hoy)")
            HourBarChart(stats.byHour, Modifier.fillMaxWidth().height(180.dp))
            Spacer(Modifier.height(16.dp))
            Text("Éxito vs fallo")
            SuccessFailChart(stats.successPct, stats.failPct, Modifier.fillMaxWidth().height(120.dp))
        }
    }
}

@Composable
fun HourBarChart(byHour: Map<Int, Int>, modifier: Modifier = Modifier) {
    val max = (byHour.values.maxOrNull() ?: 1).coerceAtLeast(1)
    Canvas(modifier) {
        val barW = size.width / 24f
        for (h in 0 until 24) {
            val v = byHour[h] ?: 0
            val hgt = (v.toFloat() / max) * size.height
            drawRect(
                color = UtcjBlue,
                topLeft = Offset(h * barW + 2f, size.height - hgt),
                size = Size(barW - 4f, hgt)
            )
        }
    }
}

@Composable
fun SuccessFailChart(success: Float, fail: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val total = (success + fail).coerceAtLeast(1f)
        val sw = size.width * (success / total)
        drawRect(AccessGreen, Offset.Zero, Size(sw, size.height))
        drawRect(AccessRed, Offset(sw, 0f), Size(size.width - sw, size.height))
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class DashboardViewModel @javax.inject.Inject constructor(
    private val log: edu.utcj.acceso.data.repository.AccessLogRepository
) : androidx.lifecycle.ViewModel() {
    data class Stats(
        val totalToday: Int = 0,
        val successPct: Float = 0f,
        val failPct: Float = 0f,
        val avgMs: Long = 0,
        val byHour: Map<Int, Int> = emptyMap()
    )

    suspend fun compute(): Stats {
        val from = TimeUtil.startOfDayMs()
        val events = log.since(from)
        if (events.isEmpty()) return Stats()
        val ok = events.count { it.result == AccessResult.ALLOWED || it.result == AccessResult.MANUAL || it.result == AccessResult.QR }
        val fail = events.count { it.result == AccessResult.DENIED }
        val durations = events.mapNotNull { it.verifyDurationMs }
        val byHour = events.groupingBy { TimeUtil.hourOfDay(it.datetimeMs) }.eachCount()
        return Stats(
            totalToday = events.size,
            successPct = 100f * ok / events.size,
            failPct = 100f * fail / events.size,
            avgMs = if (durations.isEmpty()) 0 else durations.average().toLong(),
            byHour = byHour
        )
    }
}
