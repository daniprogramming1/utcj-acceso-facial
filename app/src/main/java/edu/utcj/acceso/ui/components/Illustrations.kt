package edu.utcj.acceso.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.ui.theme.AppTheme

/*
 * Ilustraciones vectoriales dibujadas con Canvas: se adaptan al tema claro/oscuro
 * y a la paleta de BrandConfig, sin imágenes rasterizadas.
 */

private data class IlloColors(
    val blob: Color,
    val blob2: Color,
    val blob3: Color,
    val primary: Color,
    val bright: Color,
    val secondary: Color,
    val surface: Color,
    val ink: Color,
    val success: Color,
    val warning: Color,
    val skin: Color,
    val shadow: Color
)

@Composable
private fun illoColors(): IlloColors {
    val cs = MaterialTheme.colorScheme
    val ext = AppTheme.extended
    return IlloColors(
        blob = cs.primaryContainer.copy(alpha = if (ext.isDark) 0.55f else 0.9f),
        blob2 = cs.secondaryContainer.copy(alpha = 0.8f),
        blob3 = cs.tertiaryContainer.copy(alpha = 0.9f),
        primary = cs.primary,
        bright = BrandConfig.palette.primaryBright,
        secondary = cs.secondary,
        surface = cs.surface,
        ink = if (ext.isDark) Color(0xFF1E293B) else BrandConfig.palette.navy,
        success = ext.success,
        warning = ext.warning,
        skin = if (ext.isDark) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
        shadow = Color.Black.copy(alpha = if (ext.isDark) 0.35f else 0.08f)
    )
}

private fun DrawScope.backdrop(c: IlloColors) {
    val s = size.minDimension
    drawCircle(c.blob, radius = s * 0.46f, center = center)
    drawCircle(c.blob2, radius = s * 0.09f, center = center + Offset(s * 0.36f, -s * 0.31f))
    drawCircle(c.blob3, radius = s * 0.055f, center = center + Offset(-s * 0.38f, s * 0.30f))
    drawCircle(c.bright.copy(alpha = 0.5f), radius = s * 0.022f, center = center + Offset(-s * 0.30f, -s * 0.36f))
}

private fun DrawScope.checkMark(center: Offset, r: Float, color: Color, stroke: Float) {
    val p = Path().apply {
        moveTo(center.x - r * 0.45f, center.y + r * 0.02f)
        lineTo(center.x - r * 0.12f, center.y + r * 0.34f)
        lineTo(center.x + r * 0.48f, center.y - r * 0.30f)
    }
    drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.bust(center: Offset, s: Float, head: Color, body: Color) {
    // hombros
    val w = s * 0.62f
    val h = s * 0.34f
    drawRoundRect(
        body,
        topLeft = Offset(center.x - w / 2, center.y + s * 0.10f),
        size = Size(w, h),
        cornerRadius = CornerRadius(w * 0.45f, w * 0.45f)
    )
    // cabeza
    drawCircle(head, radius = s * 0.17f, center = Offset(center.x, center.y - s * 0.08f))
}

private fun DrawScope.badge(center: Offset, r: Float, color: Color, ring: Color) {
    drawCircle(ring, radius = r * 1.18f, center = center)
    drawCircle(color, radius = r, center = center)
    checkMark(center, r, Color.White, r * 0.22f)
}

private fun DrawScope.miniQr(tl: Offset, q: Float, bg: Color, ink: Color) {
    drawRoundRect(bg, tl, Size(q, q), CornerRadius(q * 0.1f))
    val cell = q / 9f
    // Patrones de posición
    listOf(Offset(1f, 1f), Offset(5f, 1f), Offset(1f, 5f)).forEach { o ->
        drawRect(ink, tl + Offset(cell * o.x, cell * o.y), Size(cell * 3, cell * 3), style = Stroke(cell * 0.8f))
        drawRect(ink, tl + Offset(cell * (o.x + 1), cell * (o.y + 1)), Size(cell, cell))
    }
    listOf(5 to 5, 6 to 6, 7 to 5, 5 to 7, 7 to 7, 6 to 4, 4 to 6).forEach { (x, y) ->
        drawRect(ink, tl + Offset(cell * x, cell * y), Size(cell, cell))
    }
}

/** Teléfono mostrando un QR de acceso con su cuenta regresiva. */
@Composable
fun QrPhoneIllustration(modifier: Modifier = Modifier) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Ilustración de QR de acceso en el teléfono" }) {
        backdrop(c)
        val s = size.minDimension
        val cw = s * 0.46f
        val ch = s * 0.66f
        val tl = Offset(center.x - cw / 2, center.y - ch / 2)
        drawRoundRect(c.shadow, tl + Offset(s * 0.02f, s * 0.03f), Size(cw, ch), CornerRadius(s * 0.07f))
        drawRoundRect(c.ink, tl, Size(cw, ch), CornerRadius(s * 0.07f))
        val inset = s * 0.025f
        drawRoundRect(
            Brush.verticalGradient(listOf(c.primary.copy(alpha = 0.25f), c.secondary.copy(alpha = 0.25f))),
            tl + Offset(inset, inset),
            Size(cw - inset * 2, ch - inset * 2),
            CornerRadius(s * 0.05f)
        )
        val q = cw * 0.66f
        miniQr(Offset(center.x - q / 2, tl.y + ch * 0.16f), q, Color.White, c.ink)
        // anillo de cuenta regresiva
        val rc = Offset(center.x, tl.y + ch * 0.80f)
        val rr = s * 0.045f
        drawCircle(Color.White.copy(alpha = 0.25f), rr, rc, style = Stroke(s * 0.012f))
        drawArc(
            c.bright, -90f, 250f, false,
            topLeft = rc - Offset(rr, rr), size = Size(rr * 2, rr * 2),
            style = Stroke(s * 0.012f, cap = StrokeCap.Round)
        )
        badge(Offset(tl.x + cw - s * 0.02f, tl.y + ch - s * 0.06f), s * 0.075f, c.success, c.surface)
    }
}

/** Escudo con candado: privacidad y cifrado. */
@Composable
fun PrivacyShieldIllustration(modifier: Modifier = Modifier) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Ilustración de privacidad" }) {
        backdrop(c)
        val s = size.minDimension
        val w = s * 0.46f
        val h = s * 0.56f
        val left = center.x - w / 2
        val top = center.y - h / 2
        val shield = Path().apply {
            moveTo(center.x, top)
            cubicTo(center.x + w * 0.22f, top + h * 0.09f, center.x + w * 0.40f, top + h * 0.10f, left + w, top + h * 0.12f)
            lineTo(left + w, top + h * 0.48f)
            cubicTo(left + w, top + h * 0.78f, center.x + w * 0.22f, top + h * 0.93f, center.x, top + h)
            cubicTo(center.x - w * 0.22f, top + h * 0.93f, left, top + h * 0.78f, left, top + h * 0.48f)
            lineTo(left, top + h * 0.12f)
            cubicTo(center.x - w * 0.40f, top + h * 0.10f, center.x - w * 0.22f, top + h * 0.09f, center.x, top)
            close()
        }
        translate(s * 0.02f, s * 0.03f) { drawPath(shield, c.shadow) }
        drawPath(shield, Brush.linearGradient(listOf(c.primary, c.secondary), Offset(left, top), Offset(left + w, top + h)))
        // candado
        val bw = w * 0.42f
        val bh = h * 0.27f
        val bt = Offset(center.x - bw / 2, center.y - bh * 0.05f)
        drawArc(
            Color.White,
            startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(center.x - bw * 0.32f, bt.y - bw * 0.36f),
            size = Size(bw * 0.64f, bw * 0.72f),
            style = Stroke(width = s * 0.03f, cap = StrokeCap.Round)
        )
        drawRoundRect(Color.White, bt, Size(bw, bh), CornerRadius(s * 0.025f))
        drawCircle(c.primary, radius = s * 0.022f, center = Offset(center.x, bt.y + bh * 0.42f))
        drawLine(c.primary, Offset(center.x, bt.y + bh * 0.42f), Offset(center.x, bt.y + bh * 0.72f), s * 0.016f, StrokeCap.Round)
        // destellos
        listOf(Offset(0.30f, -0.18f), Offset(-0.31f, 0.05f), Offset(0.27f, 0.22f)).forEach { o ->
            val p = center + Offset(o.x * s, o.y * s)
            val r = s * 0.025f
            drawLine(c.bright, p - Offset(r, 0f), p + Offset(r, 0f), s * 0.01f, StrokeCap.Round)
            drawLine(c.bright, p - Offset(0f, r), p + Offset(0f, r), s * 0.01f, StrokeCap.Round)
        }
    }
}

/** Caseta: teléfono del guardia leyendo el QR del alumno y resultado verde. */
@Composable
fun HowItWorksIllustration(modifier: Modifier = Modifier) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Ilustración del escaneo del QR en caseta" }) {
        backdrop(c)
        val s = size.minDimension
        val w = s * 0.62f
        val h = s * 0.44f
        val tl = Offset(center.x - w / 2, center.y - h / 2 - s * 0.03f)
        drawRoundRect(c.shadow, tl + Offset(s * 0.02f, s * 0.03f), Size(w, h), CornerRadius(s * 0.05f))
        drawRoundRect(c.ink, tl, Size(w, h), CornerRadius(s * 0.05f))
        drawRoundRect(c.ink.copy(alpha = 0.85f), Offset(center.x - s * 0.03f, tl.y + h), Size(s * 0.06f, s * 0.10f))
        drawRoundRect(c.ink.copy(alpha = 0.85f), Offset(center.x - s * 0.13f, tl.y + h + s * 0.09f), Size(s * 0.26f, s * 0.035f), CornerRadius(s * 0.02f))
        // recuadro de escaneo con QR
        val b = h * 0.62f
        val fc = Offset(tl.x + w * 0.34f, tl.y + h / 2)
        miniQr(Offset(fc.x - b * 0.36f, fc.y - b * 0.36f), b * 0.72f, Color.White, c.ink)
        val l = b * 0.22f
        val sw = s * 0.014f
        listOf(
            Offset(fc.x - b / 2, fc.y - b / 2) to Offset(1f, 1f),
            Offset(fc.x + b / 2, fc.y - b / 2) to Offset(-1f, 1f),
            Offset(fc.x - b / 2, fc.y + b / 2) to Offset(1f, -1f),
            Offset(fc.x + b / 2, fc.y + b / 2) to Offset(-1f, -1f)
        ).forEach { (p, d) ->
            drawLine(c.bright, p, p + Offset(l * d.x, 0f), sw, StrokeCap.Round)
            drawLine(c.bright, p, p + Offset(0f, l * d.y), sw, StrokeCap.Round)
        }
        drawLine(c.bright, Offset(fc.x - b * 0.42f, fc.y + b * 0.08f), Offset(fc.x + b * 0.42f, fc.y + b * 0.08f), s * 0.01f, StrokeCap.Round)
        // panel de resultado
        val px = tl.x + w * 0.64f
        drawRoundRect(c.success.copy(alpha = 0.22f), Offset(px, tl.y + h * 0.22f), Size(w * 0.28f, h * 0.56f), CornerRadius(s * 0.025f))
        badge(Offset(px + w * 0.14f, tl.y + h * 0.42f), s * 0.045f, c.success, c.success.copy(alpha = 0.25f))
        drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(px + w * 0.04f, tl.y + h * 0.62f), Size(w * 0.20f, s * 0.014f), CornerRadius(s * 0.01f))
    }
}

/** Alumno con birrete. */
@Composable
fun StudentIllustration(modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Ilustración de alumno" }) {
        val s = size.minDimension
        drawCircle(accent.copy(alpha = 0.14f), radius = s * 0.48f, center = center)
        bust(center + Offset(0f, s * 0.08f), s * 0.78f, c.skin, accent.copy(alpha = 0.85f))
        // birrete
        val top = center.y - s * 0.30f
        val cap = Path().apply {
            moveTo(center.x, top - s * 0.10f)
            lineTo(center.x + s * 0.26f, top)
            lineTo(center.x, top + s * 0.10f)
            lineTo(center.x - s * 0.26f, top)
            close()
        }
        drawRoundRect(c.ink, Offset(center.x - s * 0.14f, top), Size(s * 0.28f, s * 0.11f), CornerRadius(s * 0.03f))
        drawPath(cap, c.ink)
        drawLine(c.warning, Offset(center.x + s * 0.20f, top + s * 0.01f), Offset(center.x + s * 0.21f, top + s * 0.14f), s * 0.018f, StrokeCap.Round)
        drawCircle(c.warning, radius = s * 0.022f, center = Offset(center.x + s * 0.21f, top + s * 0.15f))
    }
}

/** Personal de seguridad con escudo. */
@Composable
fun GuardIllustration(modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.secondary) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Ilustración de personal de seguridad" }) {
        val s = size.minDimension
        drawCircle(accent.copy(alpha = 0.14f), radius = s * 0.48f, center = center)
        bust(center + Offset(0f, s * 0.08f), s * 0.78f, c.skin, accent.copy(alpha = 0.85f))
        // gorra
        val top = center.y - s * 0.24f
        drawArc(c.ink, 180f, 180f, true, Offset(center.x - s * 0.15f, top - s * 0.10f), Size(s * 0.30f, s * 0.22f))
        drawRoundRect(c.ink, Offset(center.x - s * 0.20f, top - s * 0.005f), Size(s * 0.40f, s * 0.04f), CornerRadius(s * 0.02f))
        // escudo
        val sc = center + Offset(s * 0.24f, s * 0.24f)
        val r = s * 0.13f
        val shield = Path().apply {
            moveTo(sc.x, sc.y - r)
            lineTo(sc.x + r * 0.85f, sc.y - r * 0.65f)
            lineTo(sc.x + r * 0.85f, sc.y)
            cubicTo(sc.x + r * 0.85f, sc.y + r * 0.6f, sc.x + r * 0.3f, sc.y + r * 0.9f, sc.x, sc.y + r)
            cubicTo(sc.x - r * 0.3f, sc.y + r * 0.9f, sc.x - r * 0.85f, sc.y + r * 0.6f, sc.x - r * 0.85f, sc.y)
            lineTo(sc.x - r * 0.85f, sc.y - r * 0.65f)
            close()
        }
        drawCircle(c.surface, radius = r * 1.25f, center = sc)
        drawPath(shield, c.success)
        checkMark(sc, r * 0.9f, Color.White, s * 0.022f)
    }
}

/** Bandeja vacía para estados sin datos. */
@Composable
fun EmptyBoxIllustration(modifier: Modifier = Modifier) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Sin elementos" }) {
        backdrop(c)
        val s = size.minDimension
        val w = s * 0.50f
        val h = s * 0.30f
        // tarjetas apiladas
        rotate(-8f, pivot = center) {
            drawRoundRect(c.surface.copy(alpha = 0.7f), Offset(center.x - w * 0.42f, center.y - h * 0.95f), Size(w * 0.84f, h * 0.7f), CornerRadius(s * 0.03f))
        }
        drawRoundRect(c.surface, Offset(center.x - w * 0.40f, center.y - h * 0.80f), Size(w * 0.80f, h * 0.75f), CornerRadius(s * 0.03f))
        drawRoundRect(c.primary.copy(alpha = 0.35f), Offset(center.x - w * 0.30f, center.y - h * 0.62f), Size(w * 0.45f, s * 0.022f), CornerRadius(s * 0.01f))
        drawRoundRect(c.primary.copy(alpha = 0.2f), Offset(center.x - w * 0.30f, center.y - h * 0.45f), Size(w * 0.30f, s * 0.022f), CornerRadius(s * 0.01f))
        // bandeja
        val tray = Path().apply {
            moveTo(center.x - w / 2, center.y - h * 0.15f)
            lineTo(center.x - w * 0.18f, center.y - h * 0.15f)
            lineTo(center.x - w * 0.12f, center.y + h * 0.05f)
            lineTo(center.x + w * 0.12f, center.y + h * 0.05f)
            lineTo(center.x + w * 0.18f, center.y - h * 0.15f)
            lineTo(center.x + w / 2, center.y - h * 0.15f)
            lineTo(center.x + w / 2, center.y + h * 0.55f)
            lineTo(center.x - w / 2, center.y + h * 0.55f)
            close()
        }
        drawPath(tray, Brush.verticalGradient(listOf(c.primary, c.secondary), center.y - h * 0.15f, center.y + h * 0.55f))
    }
}

/** Reloj para «Pendiente de aprobación». */
@Composable
fun PendingIllustration(modifier: Modifier = Modifier) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Registro pendiente de aprobación" }) {
        val s = size.minDimension
        drawCircle(c.warning.copy(alpha = 0.14f), radius = s * 0.48f, center = center)
        drawCircle(c.warning.copy(alpha = 0.22f), radius = s * 0.36f, center = center)
        drawCircle(c.surface, radius = s * 0.27f, center = center)
        drawCircle(c.warning, radius = s * 0.27f, center = center, style = Stroke(s * 0.035f))
        drawLine(c.ink, center, center + Offset(0f, -s * 0.15f), s * 0.035f, StrokeCap.Round)
        drawLine(c.ink, center, center + Offset(s * 0.11f, s * 0.05f), s * 0.035f, StrokeCap.Round)
        drawCircle(c.ink, radius = s * 0.03f, center = center)
        badge(center + Offset(s * 0.24f, s * 0.24f), s * 0.07f, c.primary, c.surface)
    }
}

/** Círculo de éxito con confeti. */
@Composable
fun SuccessIllustration(modifier: Modifier = Modifier) {
    val c = illoColors()
    Canvas(modifier.semantics { contentDescription = "Operación exitosa" }) {
        val s = size.minDimension
        drawCircle(c.success.copy(alpha = 0.12f), radius = s * 0.48f, center = center)
        drawCircle(c.success.copy(alpha = 0.22f), radius = s * 0.36f, center = center)
        drawCircle(c.success, radius = s * 0.26f, center = center)
        checkMark(center, s * 0.26f, Color.White, s * 0.05f)
        val confetti = listOf(
            Offset(-0.36f, -0.30f) to c.bright, Offset(0.38f, -0.22f) to c.warning,
            Offset(0.30f, 0.36f) to c.secondary, Offset(-0.40f, 0.20f) to c.primary,
            Offset(0.05f, -0.44f) to c.secondary
        )
        confetti.forEach { (o, col) ->
            drawRoundRect(col, center + Offset(o.x * s, o.y * s), Size(s * 0.04f, s * 0.018f), CornerRadius(s * 0.01f))
        }
    }
}
