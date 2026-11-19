package edu.utcj.acceso.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import edu.utcj.acceso.R

/*
 * Tipografía (DISEÑO.md §3), empaquetada en `res/font` (SIL OFL 1.1, funciona sin conexión):
 * - Display: Bricolage Grotesque (variable: peso + tamaño óptico) para títulos y números grandes.
 * - Lectura: Atkinson Hyperlegible (diseñada para máxima legibilidad, útil al sol en la caseta).
 */

// Fuentes variables: API experimental en Compose 1.6, estable a partir de 1.7.
@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: FontWeight, opticalSize: Float) = Font(
    R.font.bricolage_grotesque,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("opsz", opticalSize)
    )
)

/** Títulos grandes (tamaño óptico de cartel). */
val BricolageDisplay = FontFamily(
    bricolage(FontWeight.SemiBold, 48f),
    bricolage(FontWeight.Bold, 48f),
    bricolage(FontWeight.ExtraBold, 48f)
)

/** Títulos pequeños (tamaño óptico de texto, más abierto). */
val BricolageTitle = FontFamily(
    bricolage(FontWeight.SemiBold, 18f),
    bricolage(FontWeight.Bold, 18f)
)

val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold)
)

private fun style(family: FontFamily, weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

val AccesoTypography = Typography(
    displayLarge = style(BricolageDisplay, FontWeight.ExtraBold, 56, 60, -1.5),
    displayMedium = style(BricolageDisplay, FontWeight.ExtraBold, 44, 48, -1.0),
    displaySmall = style(BricolageDisplay, FontWeight.Bold, 36, 40, -0.6),
    headlineLarge = style(BricolageDisplay, FontWeight.Bold, 30, 36, -0.5),
    headlineMedium = style(BricolageDisplay, FontWeight.Bold, 26, 32, -0.4),
    headlineSmall = style(BricolageDisplay, FontWeight.Bold, 22, 28, -0.2),
    titleLarge = style(BricolageTitle, FontWeight.Bold, 20, 26, -0.1),
    titleMedium = style(AtkinsonHyperlegible, FontWeight.Bold, 16, 22, 0.0),
    titleSmall = style(AtkinsonHyperlegible, FontWeight.Bold, 14, 20, 0.1),
    bodyLarge = style(AtkinsonHyperlegible, FontWeight.Normal, 16, 24, 0.15),
    bodyMedium = style(AtkinsonHyperlegible, FontWeight.Normal, 14, 21, 0.15),
    bodySmall = style(AtkinsonHyperlegible, FontWeight.Normal, 12, 17, 0.25),
    labelLarge = style(AtkinsonHyperlegible, FontWeight.Bold, 14, 20, 0.1),
    labelMedium = style(AtkinsonHyperlegible, FontWeight.Bold, 12, 16, 0.3),
    labelSmall = style(AtkinsonHyperlegible, FontWeight.Bold, 11, 14, 0.6)
)

/** Estilos propios fuera de la escala M3. */
object AppType {
    /** Rótulo en mayúsculas espaciadas (encabezado del pase: «PASE DE ENTRADA», «MATRÍCULA»). */
    val eyebrow = style(AtkinsonHyperlegible, FontWeight.Bold, 11, 14, 1.4)

    /** Valores del pase (matrícula, carrera): números tabulares para que no «bailen». */
    val passValue = style(BricolageTitle, FontWeight.Bold, 16, 20, 0.0)
        .copy(fontFeatureSettings = "tnum")
}

/** Estilos para el kiosco: lectura a 1–2 m de distancia. */
object KioskType {
    val hero = style(BricolageDisplay, FontWeight.ExtraBold, 64, 68, -1.6)
    val title = style(BricolageDisplay, FontWeight.ExtraBold, 44, 50, -1.0)
    val subtitle = style(BricolageDisplay, FontWeight.SemiBold, 26, 32, -0.2)
    val clock = style(BricolageDisplay, FontWeight.Bold, 52, 56, -1.0).copy(fontFeatureSettings = "tnum")
    val body = style(AtkinsonHyperlegible, FontWeight.Normal, 20, 28, 0.1)
}
