package edu.utcj.acceso.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Motion

private const val LANE_BAND_FRACTION = 0.45f

/**
 * «Carril verde» (DISEÑO.md §2, idea B): una franja de luz recorre el elemento de abajo hacia
 * arriba una vez, como el semáforo de un cruce que se pone en verde. Se reproduce cada vez que
 * cambia [key]; no hace nada con movimiento reducido.
 */
fun Modifier.laneSweep(key: Any?, color: Color, enabled: Boolean = true): Modifier = composed {
    val reduced = AppTheme.reducedMotion
    val play = enabled && !reduced
    val progress = remember(key) { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(key, play) {
        if (play) progress.animateTo(1f, tween(Motion.LANE * 2, easing = Motion.emphasized))
    }
    drawWithContent {
        drawContent()
        val p = progress.value
        if (p < 1f) {
            val band = size.height * LANE_BAND_FRACTION
            val top = size.height - (size.height + band) * p
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, color.copy(alpha = 0.55f), Color.Transparent),
                    startY = top,
                    endY = top + band
                ),
                topLeft = Offset(0f, top),
                size = Size(size.width, band)
            )
        }
    }
}

/**
 * Línea perforada que se «troquela» de izquierda a derecha (Motion.PERFORATE): marca que el pase
 * ya se usó. Estática con movimiento reducido.
 */
@Composable
fun AnimatedPerforation(key: Any?, color: Color, modifier: Modifier = Modifier) {
    val reduced = AppTheme.reducedMotion
    val progress = remember(key) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(key, reduced) {
        if (!reduced) progress.animateTo(1f, tween(Motion.PERFORATE, easing = Motion.emphasizedDecelerate))
    }
    Canvas(modifier) {
        val dash = 6.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width * progress.value, size.height / 2),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
        )
    }
}
