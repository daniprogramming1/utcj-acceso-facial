package edu.utcj.acceso.ui.kiosk

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.KioskType
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import edu.utcj.acceso.util.initialsOf

private val GrantedTop = Color(0xFF0E9F6E)
private val GrantedBottom = Color(0xFF046C4E)
private val DeniedTop = Color(0xFFE02424)
private val DeniedBottom = Color(0xFF9B1C1C)

/** Resultado a pantalla completa: verde «Bienvenido» o rojo «Acceso denegado». */
@Composable
fun KioskResultOverlay(
    result: KioskViewModel.VerifyOutcome,
    secondsLeft: Int,
    totalSeconds: Int,
    assistanceRequested: Boolean,
    onFingerprint: () -> Unit,
    onQr: () -> Unit,
    onCallGuard: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = if (result.allowed) listOf(GrantedTop, GrantedBottom) else listOf(DeniedTop, DeniedBottom)
    val known = result.matricula != "—" && result.matricula != "BIOMETRIC"
    val title = if (result.allowed) "¡Bienvenido!" else "Acceso denegado"
    val a11y = buildString {
        append(title)
        if (known) append(". ${result.nombre}")
        result.reason?.let { append(". $it") }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Volver al inicio",
                onClick = onDismiss
            )
            .semantics { liveRegion = LiveRegionMode.Assertive; contentDescription = a11y }
    ) {
        val landscape = maxWidth > maxHeight
        val compact = maxHeight < 520.dp || maxWidth < 420.dp
        val large = maxWidth >= 1000.dp && maxHeight >= 640.dp
        val markSize: Dp = when {
            compact -> 120.dp
            large -> 220.dp
            else -> 184.dp
        }
        val content: @Composable (Modifier) -> Unit = { m ->
            Column(m, horizontalAlignment = if (landscape) Alignment.Start else Alignment.CenterHorizontally) {
                Text(
                    title,
                    style = if (compact) KioskType.title else KioskType.hero,
                    color = Color.White,
                    textAlign = if (landscape) TextAlign.Start else TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.md))
                if (known || result.method == AccessMethod.FINGERPRINT) {
                    PersonRow(result, large = large)
                }
                result.reason?.let {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        it,
                        style = KioskType.subtitle,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = if (landscape) TextAlign.Start else TextAlign.Center
                    )
                }
                if (!result.allowed) {
                    Spacer(Modifier.height(Spacing.xl))
                    if (assistanceRequested) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(Spacing.sm))
                            Text("Se avisó al personal de seguridad", style = KioskType.body, color = Color.White)
                        }
                    } else {
                        Text("Intenta con otra opción:", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.widthIn(max = 640.dp)) {
                            val c = Color.White.copy(alpha = 0.16f)
                            KioskOptionButton(Icons.Rounded.Fingerprint, "Huella", onFingerprint, Modifier.weight(1f), container = c)
                            KioskOptionButton(Icons.Rounded.QrCodeScanner, "QR", onQr, Modifier.weight(1f), container = c)
                            KioskOptionButton(Icons.Rounded.SupportAgent, "Guardia", onCallGuard, Modifier.weight(1f), container = c)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
                Countdown(secondsLeft, totalSeconds)
            }
        }
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = Spacing.xxl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ResultMark(result.allowed, Modifier.size(markSize * 1.2f))
                Spacer(Modifier.width(56.dp))
                content(Modifier.weight(1f, fill = false).widthIn(max = 760.dp))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ResultMark(result.allowed, Modifier.size(markSize))
                Spacer(Modifier.height(Spacing.xl))
                content(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PersonRow(result: KioskViewModel.VerifyOutcome, large: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(if (large) 84.dp else 64.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (result.method == AccessMethod.FINGERPRINT) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            } else {
                Text(initialsOf(result.nombre), color = Color.White, fontSize = if (large) 30.sp else 24.sp, style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.width(Spacing.lg))
        Column {
            Text(
                result.nombre, style = if (large) KioskType.title.copy(fontSize = 36.sp, lineHeight = 42.sp) else KioskType.subtitle, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            val meta = buildList {
                if (result.matricula != "BIOMETRIC") add("Matrícula ${result.matricula}")
                add(TimeUtil.formatTime(result.timeMs))
                add(result.method.labelEs())
            }.joinToString("  ·  ")
            Text(meta, style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun Countdown(secondsLeft: Int, totalSeconds: Int) {
    val progress by animateFloatAsState(
        secondsLeft.toFloat() / totalSeconds.coerceAtLeast(1), tween(900), label = "countdown"
    )
    Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f),
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Volviendo al inicio en $secondsLeft s · Toca para continuar",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/** Palomita o tache que se «dibuja» con animación (se muestra completo con movimiento reducido). */
@Composable
private fun ResultMark(allowed: Boolean, modifier: Modifier) {
    val reduced = AppTheme.reducedMotion
    val circle = remember { Animatable(if (reduced) 1f else 0f) }
    val stroke = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(allowed) {
        if (!reduced) {
            circle.animateTo(1f, tween(360))
            stroke.animateTo(1f, tween(380))
        }
    }
    Canvas(modifier.semantics { contentDescription = if (allowed) "Acceso permitido" else "Acceso denegado" }) {
        val r = size.minDimension / 2
        drawCircle(Color.White.copy(alpha = 0.18f * circle.value), radius = r * (0.85f + 0.15f * circle.value))
        drawCircle(Color.White, radius = r * 0.78f * circle.value)
        val tint = if (allowed) GrantedBottom else DeniedBottom
        val sw = r * 0.14f
        val measure = PathMeasure()
        val partial = Path()
        if (allowed) {
            val path = Path().apply {
                moveTo(center.x - r * 0.36f, center.y + r * 0.02f)
                lineTo(center.x - r * 0.10f, center.y + r * 0.28f)
                lineTo(center.x + r * 0.38f, center.y - r * 0.24f)
            }
            measure.setPath(path, false)
            measure.getSegment(0f, measure.length * stroke.value, partial, true)
        } else {
            // Dos trazos: el primero en la primera mitad, el segundo en la segunda.
            val first = (stroke.value * 2f).coerceIn(0f, 1f)
            val second = (stroke.value * 2f - 1f).coerceIn(0f, 1f)
            val l1 = Path().apply { moveTo(center.x - r * 0.28f, center.y - r * 0.28f); lineTo(center.x + r * 0.28f, center.y + r * 0.28f) }
            val l2 = Path().apply { moveTo(center.x + r * 0.28f, center.y - r * 0.28f); lineTo(center.x - r * 0.28f, center.y + r * 0.28f) }
            measure.setPath(l1, false); measure.getSegment(0f, measure.length * first, partial, true)
            measure.setPath(l2, false); measure.getSegment(0f, measure.length * second, partial, true)
        }
        if (circle.value > 0.6f) {
            drawPath(partial, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
