package edu.utcj.acceso.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.domain.model.ConsentRecord
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.LinkButton
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.Spacing

private val consentPoints: List<Pair<ImageVector, String>> = listOf(
    Icons.Rounded.Face to "Capturaremos de 3 a 5 muestras de tu rostro únicamente para generar un vector biométrico (embedding).",
    Icons.Rounded.HideImage to "No se almacenan fotografías: las imágenes existen solo en memoria durante la captura.",
    Icons.Rounded.EnhancedEncryption to "El vector se cifra con AES-256 en el almacén seguro del dispositivo y se usa solo para verificar tu acceso.",
    Icons.Rounded.History to "Se conserva un registro de este consentimiento con fecha, hora y versión.",
    Icons.Rounded.DeleteOutline to "Puedes eliminar tus datos en cualquier momento desde «Eliminar mis datos».",
    Icons.Rounded.Block to "El acceso puede denegarse si tu estatus institucional es BAJA o SUSPENDIDO."
)

/** Aviso de privacidad y casilla de aceptación (paso 2 del registro). */
@Composable
fun ConsentContent(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onDeleteData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Column {
            Text("Aviso de privacidad biométrica", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusPill("Versión ${ConsentRecord.CURRENT_VERSION}", Tone.Info)
                Text(BrandConfig.INSTITUTION_SHORT, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AppCard {
            Text(
                "Al continuar autorizas a ${BrandConfig.APP_NAME} a:",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.size(Spacing.md))
            consentPoints.forEachIndexed { i, (icon, text) ->
                Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.Top) {
                    IconBadge(icon, if (i == consentPoints.lastIndex) Tone.Warning else Tone.Brand, size = 32.dp)
                    Spacer(Modifier.width(Spacing.md))
                    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
        AppCard(
            modifier = Modifier.toggleable(value = accepted, role = Role.Checkbox, onValueChange = onAcceptedChange),
            color = if (accepted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepted, onCheckedChange = null)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    "He leído y acepto el tratamiento de mis datos biométricos",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            LinkButton("¿Ya tienes registro? Eliminar mis datos", onClick = onDeleteData)
        }
    }
}
