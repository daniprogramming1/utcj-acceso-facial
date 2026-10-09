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
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NoPhotography
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
    Icons.Rounded.Badge to "Usaremos solo tus datos personales (matrícula, nombre, carrera y, si lo das, tu correo) para identificarte en caseta.",
    Icons.Rounded.NoPhotography to "No usamos reconocimiento facial ni datos biométricos y no tomamos fotografías.",
    Icons.Rounded.Key to "Tu teléfono crea una llave segura que nunca sale de él; con ella firma tus códigos QR de acceso, que vencen en minutos.",
    Icons.Rounded.History to "Seguridad registra cada entrada (fecha, hora y resultado) y conserva este consentimiento con su versión.",
    Icons.Rounded.DeleteOutline to "Puedes eliminar tus datos y tu llave en cualquier momento desde «Eliminar mis datos».",
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
            Text("Aviso de privacidad", style = MaterialTheme.typography.headlineSmall)
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
                    "He leído y acepto el tratamiento de mis datos personales",
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
