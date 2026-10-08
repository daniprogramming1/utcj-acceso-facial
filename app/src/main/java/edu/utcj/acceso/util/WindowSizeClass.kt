package edu.utcj.acceso.util

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class AppWindowSize { Compact, Medium, Expanded }

/**
 * WindowSizeClass de Material 3 calculado en MainActivity con `calculateWindowSizeClass(activity)`.
 * Si no hay proveedor (previews/pruebas), se deriva de la configuración actual.
 */
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass?> { null }

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun currentWindowSizeClass(): WindowSizeClass {
    LocalWindowSizeClass.current?.let { return it }
    val config = LocalConfiguration.current
    return WindowSizeClass.calculateFromSize(DpSize(config.screenWidthDp.dp, config.screenHeightDp.dp))
}

@Composable
fun rememberAppWindowSize(): AppWindowSize = when (currentWindowSizeClass().widthSizeClass) {
    WindowWidthSizeClass.Compact -> AppWindowSize.Compact
    WindowWidthSizeClass.Medium -> AppWindowSize.Medium
    else -> AppWindowSize.Expanded
}

@Composable
fun isCompactWidth(): Boolean = rememberAppWindowSize() == AppWindowSize.Compact

/** Altura compacta (teléfono en horizontal). */
@Composable
fun isCompactHeight(): Boolean = currentWindowSizeClass().heightSizeClass == WindowHeightSizeClass.Compact

@Composable
fun isLandscape(): Boolean {
    val c = LocalConfiguration.current
    return c.screenWidthDp > c.screenHeightDp
}
