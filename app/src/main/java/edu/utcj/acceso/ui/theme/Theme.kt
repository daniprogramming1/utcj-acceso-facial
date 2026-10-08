package edu.utcj.acceso.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.domain.model.ThemeMode

private val brand = BrandConfig.palette

val LightColors: ColorScheme = lightColorScheme(
    primary = brand.primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCF2EC),
    onPrimaryContainer = Color(0xFF00382F),
    inversePrimary = brand.primaryLight,
    secondary = brand.secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6FF),
    onSecondaryContainer = Color(0xFF0A2472),
    tertiary = brand.accentGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F5E6),
    onTertiaryContainer = Color(0xFF053B25),
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral0,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral70,
    surfaceTint = brand.primary,
    inverseSurface = Neutral80,
    inverseOnSurface = Neutral10,
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Neutral40,
    outlineVariant = Neutral30,
    scrim = Color(0xFF000000),
    surfaceBright = Neutral0,
    surfaceDim = Neutral20,
    surfaceContainerLowest = Neutral0,
    surfaceContainerLow = Color(0xFFF9FAFC),
    surfaceContainer = Color(0xFFF3F6F9),
    surfaceContainerHigh = Neutral20,
    surfaceContainerHighest = Neutral30
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = brand.primaryLight,
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF00524A),
    onPrimaryContainer = Color(0xFFB2F5EA),
    inversePrimary = brand.primary,
    secondary = brand.secondaryLight,
    onSecondary = Color(0xFF0A2472),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF053B25),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = NeutralDark0,
    onBackground = Color(0xFFE6EDF5),
    surface = NeutralDark10,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = NeutralDark20,
    onSurfaceVariant = Neutral40,
    surfaceTint = brand.primaryLight,
    inverseSurface = Color(0xFFE6EDF5),
    inverseOnSurface = NeutralDark10,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    outline = NeutralDark40,
    outlineVariant = NeutralDark30,
    scrim = Color(0xFF000000),
    surfaceBright = NeutralDark30,
    surfaceDim = NeutralDark0,
    surfaceContainerLowest = NeutralDark0,
    surfaceContainerLow = Color(0xFF0F1828),
    surfaceContainer = NeutralDark10,
    surfaceContainerHigh = NeutralDark20,
    surfaceContainerHighest = NeutralDark30
)

@Composable
fun AccesoUtcjTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    reducedMotion: Boolean = systemReducedMotion(),
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    AccesoUtcjTheme(darkTheme = dark, reducedMotion = reducedMotion, content = content)
}

@Composable
fun AccesoUtcjTheme(
    darkTheme: Boolean,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AccesoTypography,
            shapes = AccesoShapes,
            content = content
        )
    }
}

/** Acceso cómodo: `AppTheme.extended.success`, `AppTheme.isDark`. */
object AppTheme {
    val extended: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}
