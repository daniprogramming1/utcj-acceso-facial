package edu.utcj.acceso.ui.panel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.ui.components.AccesoHeader
import kotlinx.coroutines.launch

@Composable
fun PendingApprovalsScreen(onBack: () -> Unit, vm: PendingViewModel = hiltViewModel()) {
    val pending by vm.pending.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Aprobaciones pendientes", onBack = onBack)
        LazyColumn(Modifier.padding(16.dp)) {
            items(pending, key = { it.matricula }) { s ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("${s.nombre} (${s.matricula})")
                    Text("Carrera: ${s.carrera}")
                    Row {
                        TextButton(onClick = { scope.launch { vm.approve(s.matricula) } }) { Text("Aprobar") }
                        TextButton(onClick = { scope.launch { vm.reject(s.matricula) } }) { Text("Rechazar") }
                    }
                }
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class PendingViewModel @javax.inject.Inject constructor(
    private val students: edu.utcj.acceso.data.repository.StudentRepository,
    private val auth: edu.utcj.acceso.data.repository.AuthRepository
) : androidx.lifecycle.ViewModel() {
    val pending = students.observePending()
    suspend fun approve(m: String) = students.approve(m, auth.currentGuardName())
    suspend fun reject(m: String) = students.reject(m, auth.currentGuardName())
}
