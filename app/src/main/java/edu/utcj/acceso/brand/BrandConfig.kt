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
    const val TAGLINE = "Tu pase de entrada, sin filas ni papeles."

    /** Contacto de soporte mostrado en «Acerca de» y en el kiosco. Confirmar con la institución. */
    const val SUPPORT_EMAIL = "soporte.acceso@utcj.edu.mx"
    const val SUPPORT_WEBSITE = "www.utcj.edu.mx"
    const val SECURITY_DESK_LABEL = "Caseta de vigilancia"

    /** Matrículas solo numéricas (UTCJ: 8 dígitos): el registro abre el teclado numérico. */
    const val MATRICULA_NUMERIC = true

    /**
     * Carreras que el alumno elige al registrarse (con «Otra carrera…» como respaldo).
     * Tomadas de los datos del proyecto (CSV de estatus y capturas): CONFIRMAR con Servicios
     * Escolares y mantener igual que la columna `carrera` del CSV institucional.
     */
    val CARRERAS = listOf(
        "Administración",
        "Contaduría",
        "Diseño Digital",
        "Energías Renovables",
        "Ingeniería en Sistemas",
        "Ingeniería en Software",
        "Ingeniería Industrial",
        "Manufactura",
        "Mecatrónica",
        "Negocios Internacionales",
        "Redes y Telecomunicaciones"
    )

    /** Zona horaria institucional para reloj del kiosco, bitácora y reportes. */
    const val TIME_ZONE_ID = "America/Ciudad_Juarez"

    @DrawableRes
    val logoRes: Int = R.drawable.ic_brand_logo

    /**
     * Paleta «Atardecer de frontera» (ver DISEÑO.md §3). El tema claro/oscuro se deriva de aquí.
     * Protagonista terracota; el teal institucional queda como acento.
     */
    val palette = BrandPalette(
        primary = Color(0xFFC2410C),       // Terracota Juárez (5.2:1 sobre blanco)
        primaryBright = Color(0xFFEA580C), // acento luminoso (gráficas, sol del pase)
        primaryLight = Color(0xFFFDBA8C),  // variante para tema oscuro
        secondary = Color(0xFF00796B),     // teal UTCJ (guiño institucional)
        secondaryLight = Color(0xFF5EEAD4),
        accentGreen = Color(0xFF22A06B),   // verde complementario del logotipo
        navy = Color(0xFF141A2E),          // Noche del desierto: kiosco / splash
        navyLight = Color(0xFF1F2742),
        ink = Color(0xFF1B1F3B)            // tinta para texto principal
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
    val navyLight: Color,
    val ink: Color
)
