package edu.utcj.acceso.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing

/** Tonos semánticos compartidos por chips, banners e íconos. */
enum class Tone { Brand, Success, Warning, Danger, Info, Neutral }

@Immutable
data class ToneColors(val container: Color, val content: Color, val accent: Color)

@Composable
fun toneColors(tone: Tone): ToneColors {
    val ext = AppTheme.extended
    val cs = MaterialTheme.colorScheme
    return when (tone) {
        Tone.Brand -> ToneColors(cs.primaryContainer, cs.onPrimaryContainer, cs.primary)
        Tone.Success -> ToneColors(ext.successContainer, ext.onSuccessContainer, ext.success)
        Tone.Warning -> ToneColors(ext.warningContainer, ext.onWarningContainer, ext.warning)
        Tone.Danger -> ToneColors(ext.dangerContainer, ext.onDangerContainer, ext.danger)
        Tone.Info -> ToneColors(ext.infoContainer, ext.onInfoContainer, ext.info)
        Tone.Neutral -> ToneColors(ext.neutralContainer, ext.onNeutralContainer, ext.onNeutralContainer)
    }
}

/** Tarjeta base: superficie con borde sutil (estilo SaaS), opcionalmente clicable. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    color: Color = MaterialTheme.colorScheme.surface,
    border: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val stroke = if (border) BorderStroke(1.dp, AppTheme.extended.cardBorder) else null
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = AppShapes.card, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = AppShapes.card, color = color, border = stroke) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Ícono dentro de un cuadro redondeado con color de tono. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tone: Tone,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null
) {
    val c = toneColors(tone)
    Box(
        modifier.size(size).background(c.container, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.accent, modifier = Modifier.size(size * 0.55f))
    }
}

/** Tendencia de un KPI (p. ej. +12 % vs ayer). */
data class KpiTrend(val text: String, val positive: Boolean)

/** Tarjeta de indicador clave: ícono, etiqueta, valor grande y tendencia/apoyo. */
@Composable
fun KpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    tone: Tone,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trend: KpiTrend? = null,
    onClick: (() -> Unit)? = null
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tone, size = 36.dp)
            Spacer(Modifier.width(Spacing.md))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
        if (trend != null || supporting != null) {
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trend != null) {
                    val ext = AppTheme.extended
                    val color = if (trend.positive) ext.success else ext.danger
                    Icon(
                        if (trend.positive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(trend.text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
                    if (supporting != null) Spacer(Modifier.width(Spacing.sm))
                }
                if (supporting != null) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Encabezado de sección con subtítulo y acción opcional a la derecha. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (action != null) action()
    }
}

/** Aviso destacado (alertas del panel, privacidad, errores). */
@Composable
fun AlertBanner(
    title: String,
    tone: Tone,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val c = toneColors(tone)
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = c.container) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.content)
                if (message != null) {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = c.content.copy(alpha = 0.85f))
                }
            }
            if (action != null) action()
        }
    }
}

/** Fila «etiqueta: valor» para hojas de detalle. */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.md))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(Spacing.md))
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
