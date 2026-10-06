package edu.utcj.acceso.ui.kiosk

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.ui.components.CameraPreview
import edu.utcj.acceso.ui.components.PrimaryBigButton
import kotlinx.coroutines.launch

@Composable
fun KioskScreen(
    onResult: (allowed: Boolean, name: String, matricula: String) -> Unit,
    onExitRequest: () -> Unit,
    vm: KioskViewModel = hiltViewModel()
) {
    val state by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).let { c ->
                c.hide(WindowInsetsCompat.Type.systemBars())
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler { onExitRequest() }

    LaunchedEffect(state.result) {
        state.result?.let { r ->
            onResult(r.allowed, r.nombre, r.matricula)
            vm.clearResult()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kiosco de acceso UTCJ", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(state.guidance, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f)) {
            CameraPreview { bmp ->
                if (!state.busy) scope.launch { vm.onFrame(bmp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryBigButton("Verificar con huella", onClick = { scope.launch { vm.verifyFingerprint(context as Activity) } })
        Spacer(Modifier.height(8.dp))
        PrimaryBigButton("Escanear QR dinámico", onClick = { vm.setModeQr() })
        Spacer(Modifier.height(8.dp))
        PrimaryBigButton("Salir (requiere contraseña)", onClick = onExitRequest)
    }
}
