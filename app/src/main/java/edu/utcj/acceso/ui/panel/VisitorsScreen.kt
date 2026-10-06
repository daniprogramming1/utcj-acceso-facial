package edu.utcj.acceso.ui.panel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.launch

@Composable
fun VisitorsScreen(onBack: () -> Unit, vm: VisitorsViewModel = hiltViewModel()) {
    val inside by vm.inside.collectAsState(initial = emptyList())
    var nombre by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var visitaA by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Visitantes", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(motivo, { motivo = it }, label = { Text("Motivo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(visitaA, { visitaA = it }, label = { Text("A quién visita") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Registrar entrada", onClick = {
                scope.launch { vm.register(nombre, motivo, visitaA); nombre = ""; motivo = ""; visitaA = "" }
            })
            Spacer(Modifier.height(16.dp))
            Text("Dentro del campus")
            LazyColumn {
                items(inside, key = { it.id }) { v ->
                    Column(Modifier.padding(8.dp)) {
                        Text("${v.nombre} → ${v.visitaA} (${TimeUtil.formatTime(v.entradaMs)})")
                        TextButton(onClick = { scope.launch { vm.exit(v.id) } }) { Text("Registrar salida") }
                    }
                }
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class VisitorsViewModel @javax.inject.Inject constructor(
    private val repo: edu.utcj.acceso.data.repository.VisitorRepository,
    private val auth: edu.utcj.acceso.data.repository.AuthRepository
) : androidx.lifecycle.ViewModel() {
    val inside = repo.observeInside()
    suspend fun register(n: String, m: String, v: String) = repo.register(n, m, v, auth.currentGuardName())
    suspend fun exit(id: Long) = repo.markExit(id)
}
