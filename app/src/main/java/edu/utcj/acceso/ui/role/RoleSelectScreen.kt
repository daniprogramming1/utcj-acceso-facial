package edu.utcj.acceso.ui.role

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.BigRoleCard

@Composable
fun RoleSelectScreen(
    onStudent: () -> Unit,
    onStudentQr: () -> Unit,
    onGuard: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Acceso UTCJ", subtitle = "Verificación de acceso universitario")
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¿Quién eres?", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            BigRoleCard(
                title = "Soy alumno",
                description = "Registro facial, consentimiento y verificación",
                accentTeal = true,
                onClick = onStudent
            )
            Spacer(Modifier.height(16.dp))
            BigRoleCard(
                title = "Soy guardia de seguridad",
                description = "Kiosco, panel, bitácora y aprobaciones",
                accentTeal = false,
                onClick = onGuard
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.TextButton(onClick = onStudentQr) {
                Text("Ya estoy registrado: mostrar mi QR dinámico")
            }
        }
    }
}
