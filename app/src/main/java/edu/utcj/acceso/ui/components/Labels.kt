package edu.utcj.acceso.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.Spacing

/* Etiquetas en español y tonos para enums del dominio. */

fun StudentStatus.labelEs(): String = when (this) {
    StudentStatus.PENDING -> "Pendiente"
    StudentStatus.APPROVED -> "Aprobado"
    StudentStatus.REJECTED -> "Rechazado"
    StudentStatus.BAJA -> "Baja"
    StudentStatus.SUSPENDIDO -> "Suspendido"
    StudentStatus.ACTIVO -> "Activo"
}

fun StudentStatus.tone(): Tone = when (this) {
    StudentStatus.APPROVED, StudentStatus.ACTIVO -> Tone.Success
    StudentStatus.PENDING -> Tone.Warning
    StudentStatus.REJECTED -> Tone.Danger
    StudentStatus.BAJA, StudentStatus.SUSPENDIDO -> Tone.Neutral
}

fun AccessResult.labelEs(): String = when (this) {
    AccessResult.ALLOWED -> "Permitido"
    AccessResult.DENIED -> "Denegado"
    AccessResult.MANUAL -> "Manual"
    AccessResult.QR -> "QR"
}

fun AccessResult.tone(): Tone = when (this) {
    AccessResult.ALLOWED -> Tone.Success
    AccessResult.DENIED -> Tone.Danger
    AccessResult.MANUAL -> Tone.Info
    AccessResult.QR -> Tone.Brand
}

fun AccessMethod.labelEs(): String = when (this) {
    AccessMethod.FACE -> "Rostro"
    AccessMethod.FINGERPRINT -> "Huella"
    AccessMethod.QR -> "QR dinámico"
    AccessMethod.MANUAL -> "Manual"
    AccessMethod.FALLBACK -> "Respaldo"
}

fun AccessMethod.icon(): ImageVector = when (this) {
    AccessMethod.FACE -> Icons.Rounded.Face
    AccessMethod.FINGERPRINT -> Icons.Rounded.Fingerprint
    AccessMethod.QR -> Icons.Rounded.QrCode2
    AccessMethod.MANUAL -> Icons.Rounded.EditNote
    AccessMethod.FALLBACK -> Icons.Rounded.Badge
}

fun IncidentCategory.labelEs(): String = when (this) {
    IncidentCategory.SEGURIDAD -> "Seguridad"
    IncidentCategory.ACCESO_NO_AUTORIZADO -> "Acceso no autorizado"
    IncidentCategory.COMPORTAMIENTO -> "Comportamiento"
    IncidentCategory.TECNICO -> "Técnico"
    IncidentCategory.OTRO -> "Otro"
}

fun IncidentCategory.tone(): Tone = when (this) {
    IncidentCategory.SEGURIDAD -> Tone.Danger
    IncidentCategory.ACCESO_NO_AUTORIZADO -> Tone.Warning
    IncidentCategory.COMPORTAMIENTO -> Tone.Info
    IncidentCategory.TECNICO -> Tone.Neutral
    IncidentCategory.OTRO -> Tone.Brand
}

/** Chip de estado con punto de color. */
@Composable
fun StatusPill(
    text: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val c = toneColors(tone)
    Row(
        modifier
            .background(c.container, AppShapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
        } else {
            Box(Modifier.size(6.dp).background(c.accent, CircleShape))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = c.content, maxLines = 1)
    }
}

@Composable
fun StudentStatusChip(status: StudentStatus, modifier: Modifier = Modifier) =
    StatusPill(status.labelEs(), status.tone(), modifier)

@Composable
fun AccessResultChip(result: AccessResult, modifier: Modifier = Modifier) =
    StatusPill(result.labelEs(), result.tone(), modifier)

@Composable
fun MethodLabel(method: AccessMethod, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Icon(method.icon(), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(method.labelEs(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
