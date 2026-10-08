package edu.utcj.acceso.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import edu.utcj.acceso.brand.BrandConfig

/*
 * Tokens de color del sistema de diseño.
 * Los colores de marca vienen de [BrandConfig.palette]; aquí se definen neutros y semánticos.
 */

private val brand = BrandConfig.palette

// Marca (atajos)
val BrandPrimary = brand.primary
val BrandPrimaryBright = brand.primaryBright
val BrandSecondary = brand.secondary
val BrandNavy = brand.navy
val BrandNavyLight = brand.navyLight

// Neutros (escala fría, ligeramente azulada)
val Neutral0 = Color(0xFFFFFFFF)
val Neutral10 = Color(0xFFF7F9FB)
val Neutral20 = Color(0xFFEEF2F6)
val Neutral30 = Color(0xFFE2E8EF)
val Neutral40 = Color(0xFFCBD5E1)
val Neutral50 = Color(0xFF94A3B8)
val Neutral60 = Color(0xFF64748B)
val Neutral70 = Color(0xFF475569)
val Neutral80 = Color(0xFF1E293B)
val Neutral90 = Color(0xFF0F172A)
val NeutralDark0 = Color(0xFF0B1220)
val NeutralDark10 = Color(0xFF111A2B)
val NeutralDark20 = Color(0xFF172236)
val NeutralDark30 = Color(0xFF213049)
val NeutralDark40 = Color(0xFF33435E)

// Semánticos
val SuccessGreen = Color(0xFF15803D)
val SuccessGreenBright = Color(0xFF22C55E)
val DangerRed = Color(0xFFC62828)
val DangerRedBright = Color(0xFFEF4444)
val WarningAmber = Color(0xFFB45309)
val WarningAmberBright = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF1D4ED8)

// Compatibilidad con código previo
val AccessGreen = SuccessGreen
val AccessRed = DangerRed
val UtcjTeal = BrandPrimary
val UtcjBlue = BrandSecondary

/**
 * Colores extendidos (fuera de Material ColorScheme): estados semánticos, gráficas y kiosco.
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
    chart1 = BrandPrimaryBright,
    chart2 = BrandSecondary,
    chart3 = DangerRedBright,
    chartGrid = Neutral30,
    heroGradientStart = Color(0xFF00695C),
    heroGradientEnd = Color(0xFF1D4ED8),
    cardBorder = Neutral30
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
    onNeutralContainer = Neutral40,
    chart1 = brand.primaryLight,
    chart2 = brand.secondaryLight,
    chart3 = Color(0xFFF87171),
    chartGrid = NeutralDark30,
    heroGradientStart = Color(0xFF004D44),
    heroGradientEnd = Color(0xFF1E3A8A),
    cardBorder = NeutralDark30,
    isDark = true
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
