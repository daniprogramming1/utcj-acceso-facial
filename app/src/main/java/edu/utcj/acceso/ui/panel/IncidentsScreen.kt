package edu.utcj.acceso.ui.panel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentsScreen(onBack: () -> Unit, vm: IncidentsViewModel = hiltViewModel()) {
    val list by vm.list.collectAsState(initial = emptyList())
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(IncidentCategory.SEGURIDAD) }
    var photo by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Incidentes", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            ExposedDropdownMenuBox(expanded, { expanded = it }) {
                OutlinedTextField(
                    cat.name, {}, readOnly = true, label = { Text("Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    IncidentCategory.entries.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = { cat = it; expanded = false })
                    }
                }
            }
            OutlinedTextField(desc, { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(photo, { photo = it }, label = { Text("URI foto (opcional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Registrar incidente", enabled = desc.isNotBlank(), onClick = {
                scope.launch { vm.report(cat, desc, photo.ifBlank { null }); desc = ""; photo = "" }
            })
            LazyColumn {
                items(list, key = { it.id }) { i ->
                    Text("${TimeUtil.formatDateTime(i.datetimeMs)} [${i.category}] ${i.description}", Modifier.padding(8.dp))
                }
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class IncidentsViewModel @javax.inject.Inject constructor(
    private val repo: edu.utcj.acceso.data.repository.IncidentRepository,
    private val auth: edu.utcj.acceso.data.repository.AuthRepository
) : androidx.lifecycle.ViewModel() {
    val list = repo.observeAll()
    suspend fun report(c: IncidentCategory, d: String, photo: String?) =
        repo.report(c, d, auth.currentGuardName(), photo)
}
