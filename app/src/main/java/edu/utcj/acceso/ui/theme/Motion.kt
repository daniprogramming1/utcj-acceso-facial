package edu.utcj.acceso.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Curvas y duraciones de movimiento (DISEÑO.md §3 «Tokens de movimiento»).
 * Todo se vuelve *fade* o estático con [LocalReducedMotion].
 */
object Motion {
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasized = CubicBezierEasing(0.3f, 0f, 0f, 1f)
    val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Presionar botones y chips. */
    const val TAP = 120
    /** Cambios de estado, aparición de tarjetas. */
    const val STATE = 200
    /** Perforación del pase al validar. */
    const val PERFORATE = 240
    /** Barrido del «carril verde». */
    const val LANE = 300
    /** Transiciones entre pantallas. */
    const val PAGE = 320
    /** Avance del sol del pase vivo entre dos ticks de la cuenta regresiva. */
    const val HORIZON_TICK = 1000

    // Escala general usada por navegación, splash y gráficas.
    const val SHORT = 180
    const val MEDIUM = PAGE
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
