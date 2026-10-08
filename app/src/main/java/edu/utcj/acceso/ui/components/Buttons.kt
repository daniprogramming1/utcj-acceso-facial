package edu.utcj.acceso.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing

private val BigButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, loading: Boolean) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current
        )
        Spacer(Modifier.width(Spacing.md))
    } else if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.sm))
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** Acción principal (relleno de marca). Alto mínimo 56 dp. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.defaultMinSize(minHeight = Spacing.buttonHeight),
        shape = AppShapes.button,
        contentPadding = BigButtonPadding
    ) { ButtonContent(text, icon, loading) }
}

/** Acción secundaria con contorno. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = Spacing.buttonHeight),
        shape = AppShapes.button,
        contentPadding = BigButtonPadding
    ) { ButtonContent(text, icon, false) }
}

/** Acción tonal (menos énfasis que primaria, más que contorno). */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = Spacing.buttonHeight),
        shape = AppShapes.button,
        contentPadding = BigButtonPadding
    ) { ButtonContent(text, icon, false) }
}

/** Acción destructiva (eliminar, rechazar). */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    val ext = AppTheme.extended
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.defaultMinSize(minHeight = Spacing.buttonHeight),
        shape = AppShapes.button,
        contentPadding = BigButtonPadding,
        colors = ButtonDefaults.buttonColors(containerColor = ext.danger, contentColor = ext.onDanger)
    ) { ButtonContent(text, icon, loading) }
}

/** Enlace/acción terciaria. */
@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    TextButton(onClick = onClick, modifier = modifier.defaultMinSize(minHeight = 48.dp), shape = AppShapes.button) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
