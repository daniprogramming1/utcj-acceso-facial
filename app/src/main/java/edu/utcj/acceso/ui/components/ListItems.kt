package edu.utcj.acceso.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.initialsOf
import kotlin.math.abs

private val AvatarPalette = listOf(
    Color(0xFF0F766E), Color(0xFF1D4ED8), Color(0xFF7C3AED), Color(0xFFB45309),
    Color(0xFF0369A1), Color(0xFF15803D), Color(0xFFBE185D), Color(0xFF4338CA)
)

/** Avatar con iniciales (sin foto). El color se deriva del nombre de forma estable. */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color? = null
) {
    val bg = color ?: AvatarPalette[abs(name.hashCode()) % AvatarPalette.size]
    val dark = AppTheme.extended.isDark
    Box(
        modifier
            .size(size)
            .background(if (dark) bg.copy(alpha = 0.35f) else bg.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initialsOf(name),
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                fontSize = (size.value * 0.36f).sp
            ),
            color = if (dark) Color.White else bg
        )
    }
}

/** Fila de lista con avatar/ícono, título, subtítulo y contenido a la derecha. */
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Spacing.md))
            trailing()
        }
    }
}
