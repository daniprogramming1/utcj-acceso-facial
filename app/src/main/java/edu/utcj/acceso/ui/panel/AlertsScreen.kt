package edu.utcj.acceso.ui.panel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.util.TimeUtil

@Composable
fun AlertsScreen(onBack: () -> Unit, vm: AlertsViewModel = hiltViewModel()) {
    var alerts by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { alerts = vm.buildAlerts() }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Alertas", onBack = onBack)
        LazyColumn(Modifier.padding(16.dp)) {
            if (alerts.isEmpty()) item { Text("Sin alertas activas") }
            items(alerts) { Text("⚠ $it", Modifier.padding(vertical = 6.dp)) }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class AlertsViewModel @javax.inject.Inject constructor(
    private val log: edu.utcj.acceso.data.repository.AccessLogRepository,
    private val settings: edu.utcj.acceso.data.repository.SettingsRepository
) : androidx.lifecycle.ViewModel() {
    suspend fun buildAlerts(): List<String> {
        val from = TimeUtil.startOfDayMs()
        val events = log.since(from)
        val out = mutableListOf<String>()
        val byMat = events.groupBy { it.matricula }
        byMat.forEach { (mat, list) ->
            val sorted = list.sortedByDescending { it.datetimeMs }
            var streak = 0
            for (e in sorted) {
                if (e.result == AccessResult.DENIED) streak++ else break
            }
            if (streak >= 3) out += "3+ fallos consecutivos: $mat"
        }
        val start = settings.getHoursStart()
        val end = settings.getHoursEnd()
        events.filter {
            it.result == AccessResult.ALLOWED && !TimeUtil.isWithinHours(it.datetimeMs, start, end)
        }.forEach {
            out += "Acceso fuera de horario (${start}:00–${end}:00): ${it.matricula} @ ${TimeUtil.formatDateTime(it.datetimeMs)}"
        }
        return out.distinct()
    }
}
