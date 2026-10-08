package edu.utcj.acceso.ui.role

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.utcj.acceso.BuildConfigProxy
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.ui.components.BrandLogoTile
import edu.utcj.acceso.ui.components.GuardIllustration
import edu.utcj.acceso.ui.components.LinkButton
import edu.utcj.acceso.ui.components.StudentIllustration
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.isCompactWidth

@Composable
fun RoleSelectScreen(
    onStudent: () -> Unit,
    onStudentAccess: () -> Unit,
    onGuard: () -> Unit
) {
    val ext = AppTheme.extended
    val compact = isCompactWidth()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Héroe con degradado institucional
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (compact) 300.dp else 280.dp)
                .background(
                    Brush.linearGradient(listOf(ext.heroGradientStart, ext.heroGradientEnd)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screenCompact),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            BrandLogoTile(size = 64.dp)
            Spacer(Modifier.height(Spacing.lg))
            Text(
                BrandConfig.APP_NAME,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                BrandConfig.TAGLINE,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xxl))
            Text(
                "¿Cómo quieres continuar?",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(Spacing.lg))

            val student: @Composable (Modifier) -> Unit = { m ->
                RoleCard(
                    title = "Soy alumno",
                    description = "Regístrate en minutos, consulta tu estatus y genera tu QR de acceso.",
                    cta = "Continuar como alumno",
                    onClick = onStudent,
                    modifier = m,
                    illustration = { StudentIllustration(Modifier.size(88.dp)) }
                )
            }
            val guard: @Composable (Modifier) -> Unit = { m ->
                RoleCard(
                    title = "Personal de seguridad",
                    description = "Modo kiosco, panel de control, bitácora, aprobaciones y reportes.",
                    cta = "Entrar al panel",
                    onClick = onGuard,
                    modifier = m,
                    illustration = { GuardIllustration(Modifier.size(88.dp)) }
                )
            }
            if (compact) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = Spacing.contentMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    student(Modifier.fillMaxWidth())
                    guard(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().widthIn(max = 960.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
                ) {
                    student(Modifier.weight(1f))
                    guard(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            LinkButton(
                "¿Ya te registraste? Consulta tu estatus y QR",
                onClick = onStudentAccess,
                icon = Icons.Rounded.QrCode2
            )
            Spacer(Modifier.height(Spacing.xl))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "Sin fotografías · Datos cifrados · v${BuildConfigProxy.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    cta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        shape = AppShapes.card,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppTheme.extended.cardBorder),
        shadowElevation = 6.dp
    ) {
        Row(Modifier.padding(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
            illustration()
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.xs))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
