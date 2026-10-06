package edu.utcj.acceso.ui.log

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton
import edu.utcj.acceso.util.TimeUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessLogScreen(
    onBack: () -> Unit,
    vm: AccessLogViewModel = hiltViewModel()
) {
    val events by vm.events.collectAsState()
    var matricula by remember { mutableStateOf("") }
    var resultFilter by remember { mutableStateOf<AccessResult?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Bitácora de acceso", subtitle = "Solo lectura", onBack = onBack)
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                matricula, {
                    matricula = it
                    vm.setFilters(matricula = it.ifBlank { null }, result = resultFilter)
                },
                label = { Text("Filtrar matrícula") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = resultFilter?.name ?: "Todos",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Resultado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Todos") }, onClick = {
                        resultFilter = null; expanded = false
                        vm.setFilters(matricula.ifBlank { null }, null)
                    })
                    AccessResult.entries.forEach { r ->
                        DropdownMenuItem(text = { Text(r.name) }, onClick = {
                            resultFilter = r; expanded = false
                            vm.setFilters(matricula.ifBlank { null }, r)
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                PrimaryBigButton("Exportar CSV", onClick = {
                    scope.launch {
                        val f = vm.exportCsv()
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
                        context.startActivity(Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    }
                }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            PrimaryBigButton("Exportar PDF", onClick = {
                scope.launch {
                    val f = vm.exportPdf()
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
                    context.startActivity(Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                }
            })
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(events, key = { it.id }) { e ->
                    Text(
                        "${TimeUtil.formatDateTime(e.datetimeMs)} · ${e.matricula} · ${e.nombre} · ${e.result} · ${e.method}",
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
