package edu.utcj.acceso.ui.theme

import androidx.compose.ui.unit.dp

/** Escala de espaciado (múltiplos de 4 dp). Úsala en lugar de valores sueltos. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp

    /** Margen horizontal de pantalla por tamaño de ventana. */
    val screenCompact = 20.dp
    val screenMedium = 28.dp
    val screenExpanded = 32.dp

    /** Ancho máximo de contenido de lectura / formularios en pantallas grandes. */
    val contentMaxWidth = 640.dp
    val formMaxWidth = 520.dp

    /** Alto mínimo de botones grandes (área táctil cómoda). */
    val buttonHeight = 56.dp
}

/** Elevaciones tonales. */
object Elevation {
    val none = 0.dp
    val low = 1.dp
    val medium = 3.dp
    val high = 6.dp
}
