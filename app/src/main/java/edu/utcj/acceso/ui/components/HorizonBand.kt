package edu.utcj.acceso.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.domain.time.DayPhase
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Motion
import edu.utcj.acceso.ui.theme.Neutral90
import edu.utcj.acceso.ui.theme.SandText
import kotlin.math.PI
import kotlin.math.sin

/** Colores del cielo, la sierra y el sol para cada momento del día. */
@Immutable
data class HorizonPalette(
    val skyTop: Color,
    val skyBottom: Color,
    val ridgeFar: Color,
    val ridgeNear: Color,
    val sun: Color,
    val label: Color,
    val labelBackground: Color,
    val stars: Boolean
)

fun horizonPaletteFor(phase: DayPhase): HorizonPalette = when (phase) {
    DayPhase.DAWN -> HorizonPalette(
        skyTop = Color(0xFFF7A8B8), skyBottom = Color(0xFFFDBA74),
        ridgeFar = Color(0xFFB45A3C), ridgeNear = Color(0xFF7C2D12),
        sun = Color(0xFFFFF1D6), label = Neutral90, labelBackground = Color.White.copy(alpha = 0.72f), stars = false
    )
    DayPhase.DAY -> HorizonPalette(
        skyTop = Color(0xFF9ED4F2), skyBottom = Color(0xFFF6E3C6),
        ridgeFar = Color(0xFFB8A189), ridgeNear = Color(0xFF8A6F55),
        sun = Color(0xFFFBBF24), label = Neutral90, labelBackground = Color.White.copy(alpha = 0.75f), stars = false
    )
    DayPhase.DUSK -> HorizonPalette(
        skyTop = Color(0xFF5B3A7A), skyBottom = Color(0xFFEA580C),
        ridgeFar = Color(0xFF5A2A3A), ridgeNear = Color(0xFF2B1622),
        sun = Color(0xFFFDBA74), label = SandText, labelBackground = Color.Black.copy(alpha = 0.35f), stars = false
    )
    DayPhase.NIGHT -> HorizonPalette(
        skyTop = Color(0xFF0B1024), skyBottom = Color(0xFF1F2742),
        ridgeFar = Color(0xFF151A30), ridgeNear = Color(0xFF070A16),
        sun = SandText, label = SandText, labelBackground = Color.White.copy(alpha = 0.12f), stars = true
    )
}

/** Silueta de la Sierra de Juárez: puntos (x, y) en fracciones del ancho y alto de la banda. */
private val FarRidge = listOf(
    0f to .62f, .08f to .50f, .16f to .56f, .27f to .38f, .36f to .50f, .45f to .44f,
    .55f to .58f, .66f to .40f, .74f to .48f, .83f to .36f, .92f to .52f, 1f to .46f
)
private val NearRidge = listOf(
    0f to .80f, .12f to .70f, .22f to .76f, .34f to .64f, .46f to .74f,
    .58f to .68f, .70f to .78f, .82f to .66f, .93f to .74f, 1f to .70f
)
private val Stars = listOf(
    .06f to .18f, .14f to .34f, .23f to .12f, .38f to .24f, .52f to .10f,
    .61f to .30f, .72f to .16f, .86f to .26f, .95f to .12f
)

/** Espacio reservado a la derecha para la etiqueta del tiempo («42 s», «1:00 min»). */
private val LabelZone = 96.dp

private const val GLOW_MIN = 0.85f
private const val GLOW_MAX = 1.15f
private const val GLOW_PERIOD_MS = 1400

/**
 * Banda de horizonte del «Pase vivo» (DISEÑO.md §2): el sol cruza la sierra mientras el QR
 * está vigente y el cielo sigue la hora real de Juárez. Con movimiento reducido el sol salta
 * de posición cada segundo y no hay halo animado; la etiqueta siempre muestra el tiempo.
 *
 * @param progress 0 = QR recién generado, 1 = a punto de renovarse (ver `horizonProgress`).
 */
@Composable
fun HorizonBand(
    progress: Float,
    phase: DayPhase,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val palette = horizonPaletteFor(phase)
    val reduced = AppTheme.reducedMotion
    val sun = remember { Animatable(progress) }
    LaunchedEffect(progress, reduced) {
        // Al renovarse el QR el sol vuelve al inicio de golpe, no «de reversa».
        if (reduced || progress < sun.value) sun.snapTo(progress)
        else sun.animateTo(progress, tween(Motion.HORIZON_TICK, easing = LinearEasing))
    }
    val glow = if (reduced) 1f else {
        val transition = rememberInfiniteTransition(label = "horizonGlow")
        transition.animateFloat(
            initialValue = GLOW_MIN,
            targetValue = GLOW_MAX,
            animationSpec = infiniteRepeatable(tween(GLOW_PERIOD_MS, easing = Motion.standard), RepeatMode.Reverse),
            label = "horizonGlow"
        ).value
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(AppShapes.horizon)
            .clearAndSetSemantics { this.contentDescription = contentDescription }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom)))
            if (palette.stars) {
                Stars.forEach { (x, y) ->
                    drawCircle(palette.sun.copy(alpha = 0.8f), radius = 1.2.dp.toPx(), center = Offset(x * size.width, y * size.height))
                }
            }
            val p = sun.value
            val radius = size.height * 0.17f
            val startX = radius * 2
            // El sol termina antes de la etiqueta del tiempo para no taparla.
            val endX = size.width - LabelZone.toPx()
            val cx = startX + (endX - startX) * p
            val cy = size.height * 0.62f - sin(PI * p).toFloat() * size.height * 0.36f
            drawCircle(palette.sun.copy(alpha = 0.28f), radius = radius * 1.9f * glow, center = Offset(cx, cy))
            drawCircle(palette.sun, radius = radius, center = Offset(cx, cy))
            drawPath(ridgePath(FarRidge, size), palette.ridgeFar)
            drawPath(ridgePath(NearRidge, size), palette.ridgeNear)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = palette.label,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(palette.labelBackground, AppShapes.pill)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

private fun ridgePath(points: List<Pair<Float, Float>>, size: Size): Path = Path().apply {
    moveTo(0f, size.height)
    points.forEach { (x, y) -> lineTo(x * size.width, y * size.height) }
    lineTo(size.width, size.height)
    close()
}
