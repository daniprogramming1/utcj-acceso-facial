package edu.utcj.acceso.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.ScreenRotationAlt
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import kotlin.math.min

/** Estado visual de la guía facial. */
enum class FaceGuideStatus { Searching, Adjust, Good, Done, Error }

@Composable
private fun statusColor(status: FaceGuideStatus): Color {
    val ext = AppTheme.extended
    return when (status) {
        FaceGuideStatus.Searching -> Color.White
        FaceGuideStatus.Adjust -> ext.warning
        FaceGuideStatus.Good -> BrandConfig.palette.primaryBright
        FaceGuideStatus.Done -> AppTheme.extended.success
        FaceGuideStatus.Error -> ext.danger
    }
}

/**
 * Capa sobre la cámara: oscurece todo menos un óvalo para el rostro, dibuja el anillo de
 * progreso (muestras capturadas) y un barrido animado mientras se analiza.
 */
@Composable
fun FaceGuideOverlay(
    status: FaceGuideStatus,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    scrim: Color = Color.Black.copy(alpha = 0.55f),
    widthFraction: Float = 0.64f
) {
    val reduced = AppTheme.reducedMotion
    val color by animateColorAsState(statusColor(status), label = "guideColor")
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(450), label = "guideProgress")
    val scanning = !reduced && (status == FaceGuideStatus.Searching || status == FaceGuideStatus.Good)
    val angle = if (scanning) {
        val t = rememberInfiniteTransition(label = "scan")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "scanAngle").value
    } else 0f
    val pulse = if (scanning) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulseA").value
    } else 0.6f

    Canvas(modifier.fillMaxSize()) {
        val ow = min(size.width * widthFraction, size.height * 0.70f / 1.32f)
        val oh = ow * 1.32f
        val tl = Offset(center.x - ow / 2, center.y - oh / 2 - size.height * 0.02f)
        // Velo con un hueco ovalado (relleno par-impar: sin capas fuera de pantalla).
        val veil = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addOval(Rect(tl, Size(ow, oh)))
        }
        drawPath(veil, scrim)

        val ringGap = 10.dp.toPx()
        val rtl = tl - Offset(ringGap, ringGap)
        val rs = Size(ow + ringGap * 2, oh + ringGap * 2)
        val sw = 5.dp.toPx()
        drawOval(Color.White.copy(alpha = 0.22f), rtl, rs, style = Stroke(sw))
        if (animatedProgress > 0f) {
            drawArc(color, -90f, 360f * animatedProgress, false, rtl, rs, style = Stroke(sw, cap = StrokeCap.Round))
        }
        if (scanning) {
            drawArc(
                color.copy(alpha = pulse), angle - 90f, 58f, false, rtl, rs,
                style = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        drawOval(color.copy(alpha = 0.85f), tl, Size(ow, oh), style = Stroke(2.dp.toPx()))
    }
}

/** Ícono sugerido para un mensaje de guía de [FaceQualityChecker]/[LivenessChecker]. */
fun guidanceIconFor(message: String): ImageVector {
    val m = message.lowercase()
    return when {
        "acércate" in m -> Icons.Rounded.ZoomIn
        "aléjate" in m -> Icons.Rounded.ZoomOut
        "luz" in m || "ilumin" in m -> Icons.Rounded.LightMode
        "estable" in m || "movid" in m -> Icons.Rounded.PanTool
        "no se detect" in m || "colócate" in m || "acércate a la cámara" in m -> Icons.Rounded.PersonSearch
        "una persona" in m -> Icons.Rounded.Groups
        "parpadea" in m -> Icons.Rounded.RemoveRedEye
        "gira" in m -> Icons.Rounded.ScreenRotationAlt
        "qr" in m -> Icons.Rounded.QrCodeScanner
        "capturada" in m || "listo" in m -> Icons.Rounded.CheckCircle
        else -> Icons.Rounded.Face
    }
}

fun guidanceToneFor(message: String): Tone {
    val m = message.lowercase()
    return when {
        "capturada" in m || "listo" in m || "perfecto" in m -> Tone.Success
        "error" in m -> Tone.Danger
        "mira de frente" in m || "colócate" in m || "buscando" in m || "muestra el" in m -> Tone.Info
        else -> Tone.Warning
    }
}

/** Chip de guía sobre la cámara (estilo «vidrio oscuro»). */
@Composable
fun GuidanceChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = guidanceIconFor(text),
    tone: Tone = guidanceToneFor(text),
    large: Boolean = false
) {
    val accent = when (tone) {
        Tone.Success -> AppTheme.extended.success
        Tone.Warning -> AppTheme.extended.warning
        Tone.Danger -> AppTheme.extended.danger
        else -> BrandConfig.palette.primaryBright
    }
    Row(
        modifier
            .background(Color.Black.copy(alpha = 0.62f), AppShapes.pill)
            .border(1.dp, Color.White.copy(alpha = 0.12f), AppShapes.pill)
            .padding(horizontal = if (large) 24.dp else 16.dp, vertical = if (large) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (large) 12.dp else 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(if (large) 30.dp else 20.dp))
        Text(
            text,
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 2
        )
    }
}

/** Puntos de progreso de muestras capturadas. */
@Composable
fun SampleDots(captured: Int, total: Int, modifier: Modifier = Modifier, onDark: Boolean = true) {
    val active = AppTheme.extended.success
    val idle = if (onDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val done = i < captured
            val scale by animateFloatAsState(if (done) 1f else 0.75f, label = "dot$i")
            Box(
                Modifier
                    .size(22.dp * scale)
                    .background(if (done) active else Color.Transparent, CircleShape)
                    .border(2.dp, if (done) active else idle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Sustituto visual de la cámara (previews, capturas de pantalla, emulador sin cámara). */
@Composable
fun CameraPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF3A4A5F), Color(0xFF1B2636), Color(0xFF0E1622))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = min(size.width, size.height)
            val c = center - Offset(0f, size.height * 0.02f)
            drawCircle(Color(0xFF8C9BB0).copy(alpha = 0.55f), radius = s * 0.13f, center = c - Offset(0f, s * 0.07f))
            drawRoundRect(
                Color(0xFF8C9BB0).copy(alpha = 0.45f),
                topLeft = Offset(c.x - s * 0.24f, c.y + s * 0.10f),
                size = Size(s * 0.48f, s * 0.36f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.2f)
            )
        }
    }
}

/**
 * Solicita el permiso de cámara con una pantalla explicativa antes de mostrar [content].
 */
@Composable
fun CameraPermissionGate(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    if (granted) {
        content()
    } else {
        val fg = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
        Box(modifier.fillMaxSize().padding(Spacing.xxl), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 420.dp)) {
                IconBadge(Icons.Rounded.CameraAlt, Tone.Brand, size = 64.dp)
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "Necesitamos acceso a la cámara",
                    style = MaterialTheme.typography.titleLarge,
                    color = fg,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    if (denied) "Permiso denegado. Puedes habilitarlo en Ajustes › Apps › ${BrandConfig.APP_NAME} › Permisos."
                    else "La cámara se usa solo para detectar tu rostro en tiempo real. Las imágenes nunca se guardan.",
                    style = TextStyle.Default.merge(MaterialTheme.typography.bodyMedium),
                    color = fg.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton("Permitir cámara", onClick = { launcher.launch(Manifest.permission.CAMERA) }, icon = Icons.Rounded.CameraAlt)
            }
        }
    }
}
