package edu.utcj.acceso.ui.scan

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.DetailRow
import edu.utcj.acceso.ui.components.IconBadge
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.LinkButton
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.StudentStatusChip
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.components.toneColors
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil

/** Franja verde «Acceso permitido» / roja «Acceso denegado» con el motivo. */
@Composable
fun AccessVerdictBanner(allowed: Boolean, reason: String?, modifier: Modifier = Modifier) {
    val c = toneColors(if (allowed) Tone.Success else Tone.Danger)
    Row(
        modifier.fillMaxWidth().background(c.accent, MaterialTheme.shapes.large).padding(Spacing.lg)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (allowed) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            contentDescription = null,
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                if (allowed) "Acceso permitido" else "Acceso denegado",
                style = MaterialTheme.typography.headlineSmall,
                color = androidx.compose.ui.graphics.Color.White
            )
            reason?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f))
            }
        }
    }
}

/** Tarjeta del alumno tras escanear su QR de acceso. */
@Composable
fun StudentScanCard(
    card: ScanCardModel,
    awaitingGuard: Boolean,
    onRegisterEntry: () -> Unit,
    onDeny: () -> Unit,
    onScanAnother: () -> Unit,
    onManualEntry: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    AppCard(modifier.fillMaxWidth()) {
        AccessVerdictBanner(card.allowed, card.reasonTitle)
        Spacer(Modifier.height(Spacing.lg))
        if (card.known) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(card.nombre, size = 80.dp)
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(card.nombre, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Matrícula ${card.matricula}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.xs))
                    card.status?.let { StudentStatusChip(it) }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            if (card.carrera.isNotBlank()) DetailRow("Carrera", card.carrera, icon = Icons.Rounded.School)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.GppBad, Tone.Danger, size = 56.dp)
                Spacer(Modifier.width(Spacing.lg))
                Text("No se identificó a ningún alumno", style = MaterialTheme.typography.titleMedium)
            }
        }
        DetailRow("Hora", TimeUtil.formatTime(card.timeMs), icon = Icons.Rounded.Schedule)
        card.reasonDetail?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.lg))
        if (awaitingGuard) {
            PrimaryButton("Registrar entrada", onClick = onRegisterEntry, icon = Icons.AutoMirrored.Rounded.Login, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton("No permitir", onClick = onDeny, icon = Icons.Rounded.Block, modifier = Modifier.fillMaxWidth())
        } else {
            PrimaryButton("Escanear otro", onClick = onScanAnother, icon = Icons.Rounded.QrCodeScanner, modifier = Modifier.fillMaxWidth())
            if (card.known && onManualEntry != null) {
                Spacer(Modifier.height(Spacing.xs))
                LinkButton("Autorizar con entrada manual", onClick = { onManualEntry(card.matricula) }, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Text(
                "Registrado en la bitácora como denegado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/** Revisión de un QR de registro: datos del alumno + Aprobar / Rechazar. */
@Composable
fun RegistrationReviewCard(
    result: PanelScanResult.Registration,
    working: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = result.payload
    AppCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.HowToReg, Tone.Brand)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("QR de registro", style = MaterialTheme.typography.titleMedium)
                Text("Revisa los datos y la identificación del alumno", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        if (p != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(p.nombre, size = 72.dp)
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Matrícula ${p.matricula}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            DetailRow("Carrera", p.carrera.ifBlank { "—" }, icon = Icons.Rounded.School)
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            DetailRow("Correo", p.correo ?: "—", icon = Icons.Rounded.Email)
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            DetailRow("Consentimiento", "v${p.consentVersion} · ${TimeUtil.formatShortDate(p.issuedAtMs)}", icon = Icons.Rounded.Policy)
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text("Estatus institucional", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                result.previousStatus?.let { StudentStatusChip(it) } ?: StatusPill("Nuevo", Tone.Info)
            }
            Spacer(Modifier.height(Spacing.md))
        }
        when (result.kind) {
            PanelScanResult.Registration.Kind.READY -> {
                if (result.replacesKey) {
                    AlertBanner(
                        title = "Cambio de teléfono",
                        message = "Este alumno ya tenía otro teléfono registrado. Al aprobar, los QR del teléfono anterior dejan de funcionar.",
                        tone = Tone.Warning,
                        icon = Icons.Rounded.PhonelinkSetup
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
                Text(
                    "Verifica su credencial antes de aprobar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton("Aprobar acceso", onClick = onApprove, icon = Icons.Rounded.Check, loading = working, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.sm))
                SecondaryButton("Rechazar", onClick = onReject, icon = Icons.Rounded.Block, enabled = !working, modifier = Modifier.fillMaxWidth())
            }
            PanelScanResult.Registration.Kind.BLOCKED -> {
                AlertBanner(
                    title = "No se puede aprobar: ${result.previousStatus?.labelEs() ?: "bloqueado"}",
                    message = "El estatus institucional (CSV) no permite el acceso. Envía al alumno a Servicios Escolares.",
                    tone = Tone.Danger,
                    icon = Icons.Rounded.Block
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton("Escanear otro", onClick = onClose, icon = Icons.Rounded.QrCodeScanner, modifier = Modifier.fillMaxWidth())
            }
            PanelScanResult.Registration.Kind.INVALID -> {
                AlertBanner(
                    title = "QR de registro no válido",
                    message = "La firma no coincide con los datos: el código fue alterado o está dañado. Pide al alumno que se registre de nuevo.",
                    tone = Tone.Danger,
                    icon = Icons.Rounded.GppBad
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton("Escanear otro", onClick = onClose, icon = Icons.Rounded.QrCodeScanner, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Sugerencia mientras no hay resultado. */
@Composable
fun ScanIdleCard(modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Text("Apunta la cámara al QR del alumno", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.md))
        listOf(
            Icons.AutoMirrored.Rounded.Login to "QR de acceso: verás su tarjeta y si puede entrar.",
            Icons.Rounded.HowToReg to "QR de registro: revisa sus datos y aprueba su acceso."
        ).forEach { (icon, text) ->
            Row(Modifier.padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(Spacing.md))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
