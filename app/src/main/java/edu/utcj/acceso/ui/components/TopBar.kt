package edu.utcj.acceso.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.data.repository.SyncStatusLabel
import edu.utcj.acceso.data.repository.SyncUiState
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing

/** Logotipo institucional definido en [BrandConfig.logoRes]. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Image(
        painter = painterResource(BrandConfig.logoRes),
        contentDescription = "Logotipo ${BrandConfig.INSTITUTION_SHORT}",
        modifier = modifier.size(size)
    )
}

/** Logotipo dentro de una «tile» redondeada, para fondos oscuros o héroes. */
@Composable
fun BrandLogoTile(modifier: Modifier = Modifier, size: Dp = 56.dp, container: Color = Color.White) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = container,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) { BrandLogo(size = size * 0.68f) }
    }
}

/** Logo + nombre de la app + institución. */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    logoSize: Dp = 40.dp,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    showInstitution: Boolean = true,
    institutionMaxLines: Int = 1
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BrandLogo(size = logoSize)
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(
                BrandConfig.APP_NAME,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor,
                maxLines = 1
            )
            if (showInstitution) {
                Text(
                    BrandConfig.INSTITUTION_NAME,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = institutionMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Barra superior compacta con logo UTCJ. Si hay [onBack] muestra la flecha; si no, el logo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccesoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    showLogo: Boolean = onBack == null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showLogo) {
                    BrandLogo(size = 32.dp)
                    Spacer(Modifier.width(Spacing.md))
                }
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

/** Indicador de conexión / sincronización: «En línea», «Sin conexión», «Pendientes: N». */
@Composable
fun SyncStatusPill(
    state: SyncUiState?,
    modifier: Modifier = Modifier,
    onDark: Boolean = false
) {
    val ext = AppTheme.extended
    val label = state?.label ?: SyncStatusLabel.ONLINE
    val text = state?.displayEs ?: "En línea"
    val dot = when (label) {
        SyncStatusLabel.ONLINE -> ext.success
        SyncStatusLabel.OFFLINE -> ext.danger
        SyncStatusLabel.SYNCING -> ext.info
        SyncStatusLabel.PENDING -> ext.warning
    }
    val bg = if (onDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .background(bg, AppShapes.pill)
            .padding(horizontal = Spacing.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(Modifier.size(8.dp).background(dot, CircleShape))
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}
