package edu.utcj.acceso.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Curvas y duraciones de movimiento. */
object Motion {
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val SHORT = 180
    const val MEDIUM = 320
    const val LONG = 520
}

/**
 * `true` si el usuario desactivó animaciones del sistema (o en capturas de pantalla).
 * Las animaciones infinitas (anillos, shimmer) se vuelven estáticas.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun systemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}
