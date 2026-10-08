package edu.utcj.acceso.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.AppWindowSize
import edu.utcj.acceso.util.rememberAppWindowSize

/** Margen horizontal de pantalla según el tamaño de ventana. */
@Composable
fun screenHorizontalPadding(): Dp = when (rememberAppWindowSize()) {
    AppWindowSize.Compact -> Spacing.screenCompact
    AppWindowSize.Medium -> Spacing.screenMedium
    AppWindowSize.Expanded -> Spacing.screenExpanded
}

/** Centra el contenido con un ancho máximo (formularios legibles en tabletas). */
@Composable
fun CenteredColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = Spacing.contentMaxWidth,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth().padding(horizontal = screenHorizontalPadding()),
            content = content
        )
    }
}
