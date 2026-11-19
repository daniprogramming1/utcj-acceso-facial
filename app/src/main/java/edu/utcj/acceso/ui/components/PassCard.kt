package edu.utcj.acceso.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing

private val NotchRadius = 12.dp
private val PerforationHeight = 24.dp
private val PassElevation = 10.dp

/**
 * Tarjeta tipo pase de abordar (DISEÑO.md §3): cabecera, línea perforada con muescas laterales
 * y cuerpo. La sombra va tintada con la marca, nunca gris.
 */
@Composable
fun PassCard(
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    val ext = AppTheme.extended
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val notchCenterY = headerHeightPx + with(LocalDensity.current) { (PerforationHeight / 2).toPx() }
    val shape = remember(notchCenterY) { PassShape(AppShapes.passCornerRadius, NotchRadius, notchCenterY) }
    Column(
        modifier
            .shadow(PassElevation, shape, ambientColor = ext.shadow.copy(alpha = 0.35f), spotColor = ext.shadow.copy(alpha = 0.45f))
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, ext.cardBorder, shape)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xl, bottom = Spacing.xs),
            content = header
        )
        Perforation(Modifier.fillMaxWidth().height(PerforationHeight))
        Column(
            Modifier.fillMaxWidth().padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xs, bottom = Spacing.xl),
            content = body
        )
    }
}

/** Línea punteada entre las dos muescas. */
@Composable
private fun Perforation(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val inset = (NotchRadius + Spacing.sm).toPx()
        val dash = 6.dp.toPx()
        drawLine(
            color = color,
            start = Offset(inset, size.height / 2),
            end = Offset(size.width - inset, size.height / 2),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
        )
    }
}

/** Rectángulo redondeado con dos muescas semicirculares a la altura [notchCenterY] (px). */
private data class PassShape(val corner: Dp, val notch: Dp, val notchCenterY: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { corner.toPx() }
        val n = with(density) { notch.toPx() }
        val base = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(c))) }
        if (notchCenterY <= n || notchCenterY >= size.height - n) return Outline.Generic(base)
        val cut = Path().apply {
            addOval(Rect(Offset(0f, notchCenterY), n))
            addOval(Rect(Offset(size.width, notchCenterY), n))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, base, cut))
    }
}
