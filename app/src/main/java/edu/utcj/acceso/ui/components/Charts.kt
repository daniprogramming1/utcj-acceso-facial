package edu.utcj.acceso.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Motion

/** Progreso 0→1 animado cuando cambian los datos (instantáneo con movimiento reducido). */
@Composable
private fun rememberChartProgress(key: Any): Float {
    val reduced = AppTheme.reducedMotion
    val anim = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(key, reduced) {
        if (reduced) {
            anim.snapTo(1f)
        } else {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(Motion.LONG + 300, easing = Motion.emphasized))
        }
    }
    return anim.value
}

private fun DrawScope.gridLines(color: Color, rows: Int, bottomPad: Float) {
    val h = size.height - bottomPad
    for (i in 0..rows) {
        val y = h * i / rows
        drawLine(
            color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
            pathEffect = if (i == rows) null else PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }
}

private fun DrawScope.label(tm: TextMeasurer, text: String, style: TextStyle, centerX: Float, top: Float) {
    val layout = tm.measure(text, style)
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, top))
}

/**
 * Barras verticales con esquinas redondeadas y degradado.
 * [labels] se dibujan cada [labelEvery] barras para evitar amontonamiento.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.extended.chart1,
    highlightIndex: Int? = null,
    labelEvery: Int = 1,
    description: String = "Gráfica de barras"
) {
    val progress = rememberChartProgress(values)
    val tm = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = AppTheme.extended.chartGrid
    val muted = color.copy(alpha = 0.35f)
    Canvas(modifier.semantics { contentDescription = description }) {
        val bottomPad = 22.dp.toPx()
        gridLines(grid, 3, bottomPad)
        if (values.isEmpty()) return@Canvas
        val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val slot = size.width / values.size
        val barW = (slot * 0.58f).coerceAtMost(28.dp.toPx())
        val chartH = size.height - bottomPad
        values.forEachIndexed { i, v ->
            val h = (v / max) * chartH * progress
            val x = i * slot + (slot - barW) / 2
            val isHi = highlightIndex == null || highlightIndex == i
            if (h > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(if (isHi) color else muted, (if (isHi) color else muted).copy(alpha = 0.55f)),
                        startY = chartH - h, endY = chartH
                    ),
                    topLeft = Offset(x, chartH - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 3, barW / 3)
                )
            }
            if (i % labelEvery == 0 && i < labels.size) {
                label(tm, labels[i], labelStyle, x + barW / 2, chartH + 6.dp.toPx())
            }
        }
    }
}

data class DonutSegment(val value: Float, val color: Color, val label: String)

/** Dona animada con texto central. */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    thickness: Dp = 18.dp
) {
    val progress = rememberChartProgress(segments.map { it.value })
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val desc = segments.joinToString { "${it.label}: ${it.value.toInt()}" }
    Box(modifier.semantics { contentDescription = "Gráfica de dona. $desc" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val d = size.minDimension - stroke
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            val total = segments.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) return@Canvas
            var start = -90f
            val gap = if (segments.count { it.value > 0 } > 1) 3f else 0f
            segments.forEach { seg ->
                val sweep = 360f * (seg.value / total) * progress
                if (sweep > 0f) {
                    drawArc(
                        seg.color, start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false, tl, Size(d, d),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.headlineSmall)
            Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Línea suavizada con área degradada y puntos. */
@Composable
fun LineChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.extended.chart2,
    description: String = "Gráfica de tendencia"
) {
    val progress = rememberChartProgress(values)
    val tm = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grid = AppTheme.extended.chartGrid
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier.semantics { contentDescription = description }) {
        val bottomPad = 22.dp.toPx()
        gridLines(grid, 3, bottomPad)
        if (values.size < 2) return@Canvas
        val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f) * 1.15f
        val chartH = size.height - bottomPad
        val padX = 12.dp.toPx()
        val stepX = (size.width - padX * 2) / (values.size - 1)
        val pts = values.mapIndexed { i, v ->
            Offset(padX + i * stepX, chartH - (v / max) * chartH * progress)
        }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val p0 = pts[i - 1]
                val p1 = pts[i]
                val cx = (p0.x + p1.x) / 2
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(pts.last().x, chartH)
            lineTo(pts.first().x, chartH)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)), 0f, chartH))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        pts.forEachIndexed { i, p ->
            val last = i == pts.lastIndex
            drawCircle(surface, radius = if (last) 6.dp.toPx() else 4.dp.toPx(), center = p)
            drawCircle(color, radius = if (last) 6.dp.toPx() else 4.dp.toPx(), center = p, style = Stroke(2.5.dp.toPx()))
            if (i < labels.size) label(tm, labels[i], labelStyle, p.x, chartH + 6.dp.toPx())
        }
    }
}

/** Leyenda simple: punto de color + etiqueta + valor. */
@Composable
fun ChartLegendItem(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 8.dp).size(10.dp).background(color, CircleShape))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}
