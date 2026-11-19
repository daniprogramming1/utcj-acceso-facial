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

/** Claro: arena + terracota, texto en tinta «Noche del desierto». */
val LightColors: ColorScheme = lightColorScheme(
    primary = brand.primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4D3),
    onPrimaryContainer = Color(0xFF5A1A04),
    inversePrimary = brand.primaryLight,
    secondary = brand.secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF2EC),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = brand.ink,
    onTertiary = SandText,
    tertiaryContainer = Color(0xFFE4E3EE),
    onTertiaryContainer = brand.ink,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral0,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral70,
    surfaceTint = brand.primary,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral10,
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Neutral50,
    outlineVariant = Neutral30,
    scrim = Color(0xFF000000),
    surfaceBright = Neutral0,
    surfaceDim = Neutral20,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFBF6),
    surfaceContainer = Color(0xFFF8F2EA),
    surfaceContainerHigh = Neutral20,
    surfaceContainerHighest = Neutral30
)

/** Oscuro: noche del desierto con texto arena. */
val DarkColors: ColorScheme = darkColorScheme(
    primary = brand.primaryLight,
    onPrimary = Color(0xFF4A1503),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFE4D3),
    inversePrimary = brand.primary,
    secondary = brand.secondaryLight,
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF00524A),
    onSecondaryContainer = Color(0xFFB2F5EA),
    tertiary = Color(0xFFC7C9E6),
    onTertiary = brand.ink,
    tertiaryContainer = Color(0xFF2F3355),
    onTertiaryContainer = Color(0xFFE4E3EE),
    background = NeutralDark0,
    onBackground = SandText,
    surface = NeutralDark10,
    onSurface = SandText,
    surfaceVariant = NeutralDark20,
    onSurfaceVariant = SandTextMuted,
    surfaceTint = brand.primaryLight,
    inverseSurface = SandText,
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
    surfaceContainerLowest = Color(0xFF0D0E16),
    surfaceContainerLow = Color(0xFF161722),
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
