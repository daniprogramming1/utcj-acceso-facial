package edu.utcj.acceso.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Escala M3. Los radios cambian según la pieza (DISEÑO.md §3): nada de «todo redondeado igual». */
val AccesoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Formas con nombre semántico. */
object AppShapes {
    val button = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = RoundedCornerShape(percent = 50)
    val field = RoundedCornerShape(12.dp)

    /** Radio de las esquinas del pase (PassCard dibuja además las muescas). */
    val passCornerRadius = 28.dp

    /** Banda de horizonte del pase vivo. */
    val horizon = RoundedCornerShape(16.dp)
}
