package edu.utcj.acceso.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing

/* ---------- Estados vacíos ---------- */

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    illustration: @Composable () -> Unit = { EmptyBoxIllustration(Modifier.size(140.dp)) },
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Spacing.xxxl, horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        illustration()
        Spacer(Modifier.height(Spacing.xl))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.lg))
            action()
        }
    }
}

/* ---------- Skeleton / shimmer ---------- */

/** Brillo animado para placeholders de carga (estático si hay movimiento reducido). */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    if (AppTheme.reducedMotion) {
        background(base)
    } else {
        val t = rememberInfiniteTransition(label = "shimmer")
        val x by t.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
            label = "shimmerX"
        )
        drawWithContent {
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(w * x - w, 0f),
                    end = Offset(w * x, size.height)
                )
            )
        }
    }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, height: Dp = 14.dp, corner: Dp = 8.dp) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .shimmer()
    )
}

@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).shimmer())
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.6f))
            SkeletonBlock(Modifier.fillMaxWidth(0.35f), height = 10.dp)
        }
    }
}

@Composable
fun SkeletonList(modifier: Modifier = Modifier, count: Int = 6) {
    Column(modifier) { repeat(count) { SkeletonListItem() } }
}

/* ---------- Diálogos ---------- */

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancelar",
    destructive: Boolean = false,
    icon: ImageVector = Icons.Rounded.Warning
) {
    val ext = AppTheme.extended
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            IconBadge(icon, if (destructive) Tone.Danger else Tone.Brand, size = 48.dp)
        },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = if (destructive) ext.danger else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
        shape = MaterialTheme.shapes.extraLarge
    )
}

/* ---------- Snackbars ---------- */

@Composable
fun AppSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary
        )
    }
}

/* ---------- Indicador de pasos ---------- */

@Composable
fun StepIndicator(steps: List<String>, current: Int, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            steps.forEachIndexed { i, _ ->
                val done = i < current
                val active = i == current
                val circleColor = when {
                    done -> cs.primary
                    active -> cs.primary
                    else -> cs.surfaceContainerHighest
                }
                Box(
                    Modifier.size(28.dp).background(circleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Paso completado",
                            tint = cs.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) cs.onPrimary else cs.onSurfaceVariant
                        )
                    }
                }
                if (i < steps.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .height(3.dp)
                            .background(if (i < current) cs.primary else cs.surfaceContainerHighest, AppShapes.pill)
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Paso ${current + 1} de ${steps.size} · ${steps.getOrElse(current) { "" }}",
            style = MaterialTheme.typography.labelLarge,
            color = cs.primary
        )
    }
}
