package edu.utcj.acceso.ui.guard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import edu.utcj.acceso.ui.components.SectionTitle

@Composable
fun GuardHomeScreen(
    onKiosk: () -> Unit,
    onLog: () -> Unit,
    onSearch: () -> Unit,
    onPending: () -> Unit,
    onManual: () -> Unit,
    onVisitors: () -> Unit,
    onIncidents: () -> Unit,
    onDashboard: () -> Unit,
    onAlerts: () -> Unit,
    onSettings: () -> Unit,
    onChangePassword: () -> Unit,
    onEval: () -> Unit,
    onLogout: () -> Unit,
    vm: GuardHomeViewModel = hiltViewModel()
) {
    val sync by vm.sync.collectAsState(initial = null)
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(
            title = "Panel de guardia",
            subtitle = vm.guardName(),
            syncLabel = sync?.displayEs
        )
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            SectionTitle("Operación")
            PrimaryBigButton("Modo kiosco", onClick = onKiosk)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Bitácora de acceso", onClick = onLog)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Entrada manual", onClick = onManual)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Visitantes", onClick = onVisitors)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Incidentes", onClick = onIncidents)
            Spacer(Modifier.height(16.dp))
            SectionTitle("Alumnos")
            PrimaryBigButton("Buscar alumnos", onClick = onSearch)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Aprobaciones pendientes", onClick = onPending)
            Spacer(Modifier.height(16.dp))
            SectionTitle("Análisis")
            PrimaryBigButton("Tablero", onClick = onDashboard)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Alertas", onClick = onAlerts)
            Spacer(Modifier.height(16.dp))
            SectionTitle("Sistema")
            PrimaryBigButton("Configuración", onClick = onSettings)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Cambiar contraseña", onClick = onChangePassword)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Modo evaluación (oculto)", onClick = onEval)
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Cerrar sesión", onClick = {
                vm.logout()
                onLogout()
            })
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class GuardHomeViewModel @javax.inject.Inject constructor(
    private val auth: edu.utcj.acceso.data.repository.AuthRepository,
    private val syncRepo: edu.utcj.acceso.data.repository.SyncRepository
) : androidx.lifecycle.ViewModel() {
    val sync = syncRepo.observeStatus()
    fun guardName() = auth.currentGuardName()
    fun logout() = auth.logout()
}
