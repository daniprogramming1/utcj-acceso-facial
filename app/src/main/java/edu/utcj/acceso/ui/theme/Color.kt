package edu.utcj.acceso.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import edu.utcj.acceso.brand.BrandConfig

/*
 * Tokens de color «Atardecer de frontera» (DISEÑO.md §3).
 * Los colores de marca vienen de [BrandConfig.palette]; aquí se definen neutros y semánticos.
 */

private val brand = BrandConfig.palette

// Marca (atajos)
val BrandPrimary = brand.primary
val BrandSecondary = brand.secondary
val BrandNavy = brand.navy

// Neutros arena (cálidos, nunca grises puros). 90 = tinta «Noche del desierto».
val Neutral0 = Color(0xFFFFFDF9)
val Neutral10 = Color(0xFFFBF7F2)
val Neutral20 = Color(0xFFF3EBE1)
val Neutral30 = Color(0xFFE7DCCD)
val Neutral40 = Color(0xFFD3C3AF)
val Neutral50 = Color(0xFF9A8670) // ≥ 3:1 sobre fondo: bordes de campos
val Neutral60 = Color(0xFF85725F)
val Neutral70 = Color(0xFF6B5A48) // texto secundario (6.4:1 sobre fondo)
val Neutral80 = Color(0xFF2E241B)
val Neutral90 = brand.ink

// Noche del desierto (tema oscuro)
val NeutralDark0 = Color(0xFF12131C)
val NeutralDark10 = Color(0xFF1A1B26)
val NeutralDark20 = Color(0xFF232532)
val NeutralDark30 = Color(0xFF2F3141)
val NeutralDark40 = Color(0xFF6E7086) // ≥ 3:1 sobre fondo oscuro: bordes de campos
val SandText = Color(0xFFF3EBE1)
val SandTextMuted = Color(0xFFCDBFAE)

// Semánticos
val SuccessGreen = Color(0xFF15803D)
val SuccessGreenBright = Color(0xFF22C55E)
val DangerRed = Color(0xFFB91C1C)
val DangerRedBright = Color(0xFFEF4444)
val WarningAmber = Color(0xFFB45309)
val WarningAmberBright = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF1D4ED8)

/**
 * Colores extendidos (fuera de Material ColorScheme): estados semánticos, gráficas,
 * degradado del encabezado y sombra tintada (las sombras nunca son grises).
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chartGrid: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val cardBorder: Color,
    /** Color de sombra (ambient/spot) tintado con la marca. */
    val shadow: Color,
    val isDark: Boolean = false
)

val LightExtendedColors = ExtendedColors(
    success = SuccessGreen,
    onSuccess = Color.White,
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF14532D),
    warning = WarningAmber,
    onWarning = Color.White,
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
    danger = DangerRed,
    onDanger = Color.White,
    dangerContainer = Color(0xFFFEE2E2),
    onDangerContainer = Color(0xFF7F1D1D),
    info = InfoBlue,
    infoContainer = Color(0xFFDBEAFE),
    onInfoContainer = Color(0xFF1E3A8A),
    neutralContainer = Neutral20,
    onNeutralContainer = Neutral70,
    chart1 = brand.primaryBright,
    chart2 = brand.secondary,
    chart3 = DangerRedBright,
    chartGrid = Neutral30,
    heroGradientStart = brand.ink,
    heroGradientEnd = Color(0xFF7C2D12), // noche → brasa del atardecer
    cardBorder = Neutral30,
    shadow = Color(0xFF7C2D12)
)

val DarkExtendedColors = ExtendedColors(
    success = SuccessGreenBright,
    onSuccess = Color(0xFF052E16),
    successContainer = Color(0xFF14532D),
    onSuccessContainer = Color(0xFFBBF7D0),
    warning = WarningAmberBright,
    onWarning = Color(0xFF451A03),
    warningContainer = Color(0xFF713F12),
    onWarningContainer = Color(0xFFFDE68A),
    danger = Color(0xFFF87171),
    onDanger = Color(0xFF450A0A),
    dangerContainer = Color(0xFF7F1D1D),
    onDangerContainer = Color(0xFFFECACA),
    info = Color(0xFF93C5FD),
    infoContainer = Color(0xFF1E3A8A),
    onInfoContainer = Color(0xFFDBEAFE),
    neutralContainer = NeutralDark30,
    onNeutralContainer = SandTextMuted,
    chart1 = brand.primaryLight,
    chart2 = brand.secondaryLight,
    chart3 = Color(0xFFF87171),
    chartGrid = NeutralDark30,
    heroGradientStart = Color(0xFF0D0E16),
    heroGradientEnd = Color(0xFF5A1F0B),
    cardBorder = NeutralDark30,
    shadow = Color.Black,
    isDark = true
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
