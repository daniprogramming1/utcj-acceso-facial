package edu.utcj.acceso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = UtcjBlue,
    onPrimary = Color.White,
    primaryContainer = UtcjBlueLight,
    secondary = UtcjTeal,
    onSecondary = Color.White,
    secondaryContainer = UtcjTealLight,
    tertiary = UtcjTealDark,
    background = SurfaceLight,
    surface = Color.White,
    onBackground = OnSurface,
    onSurface = OnSurface,
    error = AccessRed
)

private val DarkColors = darkColorScheme(
    primary = UtcjBlueLight,
    onPrimary = Color.Black,
    secondary = UtcjTealLight,
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = AccessRed
)

@Composable
fun AccesoUtcjTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AccesoTypography,
        content = content
    )
}
