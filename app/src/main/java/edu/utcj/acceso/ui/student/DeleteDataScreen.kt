package edu.utcj.acceso.ui.student

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
fun DeleteDataScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: DeleteDataViewModel = hiltViewModel()
) {
    var matricula by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Eliminar mis datos", onBack = onBack)
        Column(Modifier.padding(24.dp)) {
            Text("Se eliminarán tu registro y embeddings cifrados. Esta acción no se puede deshacer.")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = matricula,
                onValueChange = { matricula = it },
                label = { Text("Matrícula") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            PrimaryBigButton(text = "Eliminar definitivamente", onClick = {
                scope.launch {
                    vm.delete(matricula.trim())
                    message = "Datos eliminados (si existían)."
                    onDone()
                }
            })
            message?.let { Text(it) }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class DeleteDataViewModel @javax.inject.Inject constructor(
    private val students: edu.utcj.acceso.data.repository.StudentRepository
) : androidx.lifecycle.ViewModel() {
    suspend fun delete(matricula: String) = students.deleteStudentData(matricula)
}
