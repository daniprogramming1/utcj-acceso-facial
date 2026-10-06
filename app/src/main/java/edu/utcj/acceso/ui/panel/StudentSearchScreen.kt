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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import kotlinx.coroutines.launch

@Composable
fun StudentSearchScreen(onBack: () -> Unit, vm: StudentSearchViewModel = hiltViewModel()) {
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Student>>(emptyList()) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Buscar alumnos", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(q, { q = it }, label = { Text("Nombre o matrícula") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Buscar", onClick = { scope.launch { results = vm.search(q) } })
            LazyColumn {
                items(results, key = { it.matricula }) { s ->
                    Text("${s.matricula} — ${s.nombre} [${s.status}]", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class StudentSearchViewModel @javax.inject.Inject constructor(
    private val repo: edu.utcj.acceso.data.repository.StudentRepository
) : androidx.lifecycle.ViewModel() {
    suspend fun search(q: String) = repo.search(q)
}
