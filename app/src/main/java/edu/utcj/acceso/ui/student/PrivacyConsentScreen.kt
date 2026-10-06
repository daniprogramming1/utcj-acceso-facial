package edu.utcj.acceso.ui.student

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.domain.model.ConsentRecord
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton

@Composable
fun PrivacyConsentScreen(
    onAccept: () -> Unit,
    onBack: () -> Unit,
    onDeleteData: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Consentimiento de privacidad", onBack = onBack)
        Column(
            Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
        ) {
            Text(
                "Versión del consentimiento: ${ConsentRecord.CURRENT_VERSION}",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(12.dp))
            Text(
                """
                Al continuar, autorizas a Acceso UTCJ a:
                
                • Capturar muestras de tu rostro únicamente para generar un vector biométrico cifrado (embedding).
                • NO almacenar fotografías originales de tu rostro.
                • Usar esos embeddings cifrados solo para verificación de acceso en campus.
                • Conservar un registro de consentimiento con fecha/hora y versión.
                
                Puedes solicitar la eliminación de tus datos en cualquier momento con «Eliminar mis datos».
                
                El acceso puede denegarse si tu estatus institucional es BAJA o SUSPENDIDO.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Text("He leído y acepto el tratamiento de mis datos biométricos")
            }
            Spacer(Modifier.height(24.dp))
            PrimaryBigButton(
                text = "Aceptar",
                enabled = checked,
                onClick = onAccept
            )
            Spacer(Modifier.height(12.dp))
            PrimaryBigButton(text = "Eliminar mis datos", onClick = onDeleteData)
        }
    }
}
