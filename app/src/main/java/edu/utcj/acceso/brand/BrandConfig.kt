package edu.utcj.acceso.brand

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import edu.utcj.acceso.R

/**
 * Configuración de marca (white-label).
 *
 * TODO el branding visible de la app sale de aquí: nombre, institución, contacto,
 * logotipo y paleta. Para revender la app a otra escuela:
 *  1. Cambia los textos y colores de este archivo.
 *  2. Reemplaza `res/drawable/ic_brand_logo.xml` (logo a color) y
 *     `res/drawable/ic_launcher_foreground.xml` / `ic_launcher_monochrome.xml` (ícono).
 *  3. Ajusta `app_name` en `res/values/strings.xml` y `brand_*` en `res/values/colors.xml`
 *     (los usa el splash nativo, que se pinta antes de Compose).
 *  4. Cambia `applicationId` en `app/build.gradle.kts` si se publica como app independiente.
 *
 * Ver «Guía de rebranding» en README.md.
 */
object BrandConfig {
    /** Nombre comercial mostrado en encabezados (debe coincidir con `app_name`). */
    const val APP_NAME = "Acceso UTCJ"

    /** Nombre del producto (plataforma) que se licencia a instituciones. */
    const val PRODUCT_NAME = "Acceso QR"

    const val INSTITUTION_NAME = "Universidad Tecnológica de Ciudad Juárez"
    const val INSTITUTION_SHORT = "UTCJ"
    const val TAGLINE = "Control de acceso inteligente, rápido y privado"

    /** Contacto de soporte mostrado en «Acerca de» y en el kiosco. Confirmar con la institución. */
    const val SUPPORT_EMAIL = "soporte.acceso@utcj.edu.mx"
    const val SUPPORT_WEBSITE = "www.utcj.edu.mx"
    const val SECURITY_DESK_LABEL = "Caseta de vigilancia"

    /** Zona horaria institucional para reloj del kiosco, bitácora y reportes. */
    const val TIME_ZONE_ID = "America/Ciudad_Juarez"

    @DrawableRes
    val logoRes: Int = R.drawable.ic_brand_logo

    /** Paleta institucional. El tema claro/oscuro se deriva de estos valores. */
    val palette = BrandPalette(
        primary = Color(0xFF00796B),       // verde azulado del logotipo
        primaryBright = Color(0xFF14B8A6), // acento luminoso (anillos, gráficas)
        primaryLight = Color(0xFF5EEAD4),  // variante para tema oscuro
        secondary = Color(0xFF1D4ED8),     // azul institucional
        secondaryLight = Color(0xFF93B4FF),
        accentGreen = Color(0xFF22A06B),   // verde complementario del logotipo
        navy = Color(0xFF0B1F3A),          // fondo del kiosco / splash
        navyLight = Color(0xFF12315A)
    )
}

data class BrandPalette(
    val primary: Color,
    val primaryBright: Color,
    val primaryLight: Color,
    val secondary: Color,
    val secondaryLight: Color,
    val accentGreen: Color,
    val navy: Color,
    val navyLight: Color
)
