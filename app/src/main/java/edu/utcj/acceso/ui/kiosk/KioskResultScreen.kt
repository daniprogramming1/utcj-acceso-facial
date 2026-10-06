package edu.utcj.acceso.ui.kiosk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.utcj.acceso.ui.components.PrimaryBigButton
import edu.utcj.acceso.ui.theme.AccessGreen
import edu.utcj.acceso.ui.theme.AccessRed
import kotlinx.coroutines.delay

@Composable
fun KioskResultScreen(
    allowed: Boolean,
    name: String,
    matricula: String,
    onReset: () -> Unit,
    idleMs: Long = 30_000L
) {
    LaunchedEffect(allowed, name, matricula) {
        delay(idleMs)
        onReset()
    }
    Surface(color = if (allowed) AccessGreen else AccessRed, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (allowed) "ACCESO PERMITIDO" else "ACCESO DENEGADO",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(name, fontSize = 28.sp, color = Color.White)
            Text(matricula, fontSize = 22.sp, color = Color.White)
            Spacer(Modifier.height(32.dp))
            PrimaryBigButton("Continuar", onClick = onReset)
        }
    }
}
