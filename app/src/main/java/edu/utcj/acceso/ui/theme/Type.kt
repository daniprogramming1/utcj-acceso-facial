package edu.utcj.acceso.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import edu.utcj.acceso.R

/**
 * Plus Jakarta Sans (SIL Open Font License 1.1), empaquetada en `res/font`:
 * funciona 100 % sin conexión (no usa fuentes descargables).
 */
val JakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

private fun style(weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = JakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

val AccesoTypography = Typography(
    displayLarge = style(FontWeight.ExtraBold, 56, 64, -1.0),
    displayMedium = style(FontWeight.ExtraBold, 44, 52, -0.75),
    displaySmall = style(FontWeight.Bold, 36, 44, -0.5),
    headlineLarge = style(FontWeight.Bold, 30, 38, -0.4),
    headlineMedium = style(FontWeight.Bold, 26, 34, -0.3),
    headlineSmall = style(FontWeight.Bold, 22, 30, -0.2),
    titleLarge = style(FontWeight.Bold, 20, 28, -0.1),
    titleMedium = style(FontWeight.SemiBold, 16, 24, 0.0),
    titleSmall = style(FontWeight.SemiBold, 14, 20, 0.0),
    bodyLarge = style(FontWeight.Normal, 16, 24, 0.1),
    bodyMedium = style(FontWeight.Normal, 14, 21, 0.1),
    bodySmall = style(FontWeight.Normal, 12, 17, 0.2),
    labelLarge = style(FontWeight.SemiBold, 14, 20, 0.1),
    labelMedium = style(FontWeight.SemiBold, 12, 16, 0.3),
    labelSmall = style(FontWeight.SemiBold, 11, 14, 0.5)
)

/** Estilos para el kiosco: lectura a 1–2 m de distancia. */
object KioskType {
    val hero = style(FontWeight.ExtraBold, 64, 70, -1.2)
    val title = style(FontWeight.ExtraBold, 44, 52, -0.8)
    val subtitle = style(FontWeight.SemiBold, 26, 34, -0.2)
    val clock = style(FontWeight.Bold, 52, 58, -1.0)
    val body = style(FontWeight.Medium, 20, 28, 0.0)
}
