package edu.utcj.acceso.ui.panel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import kotlinx.coroutines.launch

@Composable
fun ManualEntryScreen(onBack: () -> Unit, vm: ManualEntryViewModel = hiltViewModel()) {
    var matricula by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Entrada manual", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(matricula, { matricula = it }, label = { Text("Matrícula") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(reason, { reason = it }, label = { Text("Motivo (obligatorio)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryBigButton("Registrar acceso", enabled = reason.isNotBlank() && matricula.isNotBlank(), onClick = {
                scope.launch {
                    vm.log(matricula, nombre.ifBlank { matricula }, reason)
                    msg = "Registrado"
                }
            })
            msg?.let { Text(it) }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ManualEntryViewModel @javax.inject.Inject constructor(
    private val log: edu.utcj.acceso.data.repository.AccessLogRepository,
    private val auth: edu.utcj.acceso.data.repository.AuthRepository
) : androidx.lifecycle.ViewModel() {
    suspend fun log(mat: String, nombre: String, reason: String) {
        log.log(
            mat, nombre,
            edu.utcj.acceso.domain.model.AccessResult.MANUAL,
            edu.utcj.acceso.domain.model.AccessMethod.MANUAL,
            guard = auth.currentGuardName(),
            reason = reason
        )
    }
}
