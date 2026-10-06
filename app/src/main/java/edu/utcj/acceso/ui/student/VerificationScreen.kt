package edu.utcj.acceso.ui.student

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton

@Composable
fun VerificationScreen(
    onShowQr: () -> Unit,
    onGoKioskHint: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Registro enviado", subtitle = "Estado: PENDIENTE", onBack = onBack)
        Column(Modifier.padding(24.dp)) {
            Text("Tu registro queda en estado PENDIENTE hasta que un guardia lo apruebe.")
            Spacer(Modifier.height(12.dp))
            Text("Solo se guardaron vectores biométricos cifrados; ninguna fotografía.")
            Spacer(Modifier.height(12.dp))
            Text("Una vez aprobado, el kiosco podrá reconocerte por rostro. Si el rostro falla, usa el QR dinámico (válido 30 s) o la huella.")
            Spacer(Modifier.height(24.dp))
            PrimaryBigButton(text = "Mostrar mi QR dinámico", onClick = onShowQr)
            Spacer(Modifier.height(12.dp))
            PrimaryBigButton(text = "Volver al inicio", onClick = onGoKioskHint)
        }
    }
}
