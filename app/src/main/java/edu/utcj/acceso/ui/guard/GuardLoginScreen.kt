package edu.utcj.acceso.ui.guard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.brand.BrandConfig
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.security.AuthOutcome
import edu.utcj.acceso.data.security.GuardAuthManager
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppTextField
import edu.utcj.acceso.ui.components.BrandLogoTile
import edu.utcj.acceso.ui.components.PasswordField
import edu.utcj.acceso.ui.components.PasswordStrengthMeter
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.theme.AppShapes
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class GuardAuthViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {
    fun isPasswordSet() = auth.isPasswordSet()
    fun login(p: CharArray) = auth.login(p)
    fun setup(p: CharArray, name: String) = auth.setupPassword(p, name)
    fun change(c: CharArray, n: CharArray) = auth.changePassword(c, n)
    fun reauth(p: CharArray) = auth.requireReauth(p)
    fun lockRemainingMs() = auth.lockRemainingMs()
    fun guardName() = auth.currentGuardName()
}

/** Estado de la pantalla de acceso del guardia. */
data class LoginUi(
    val password: String = "",
    val error: String? = null,
    val lockRemainingMs: Long = 0L,
    val loading: Boolean = false
) {
    val locked: Boolean get() = lockRemainingMs > 0
}

private fun AuthOutcome.toError(): Pair<String?, Long> = when (this) {
    is AuthOutcome.Success -> null to 0L
    is AuthOutcome.Error -> message to 0L
    is AuthOutcome.Locked -> "Demasiados intentos fallidos." to remainingMs
}

/** Hace avanzar la cuenta regresiva del bloqueo cada segundo. */
@Composable
private fun rememberLockCountdown(initialMs: Long, source: () -> Long): Long {
    var remaining by remember { mutableLongStateOf(initialMs) }
    LaunchedEffect(initialMs) {
        remaining = initialMs
        while (remaining > 0) {
            delay(1_000)
            remaining = source()
        }
    }
    return remaining
}

@Composable
fun GuardLoginScreen(
    onSuccess: () -> Unit,
    onNeedSetup: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    if (!vm.isPasswordSet()) {
        LaunchedEffect(Unit) { onNeedSetup() }
        return
    }
    var ui by remember { mutableStateOf(LoginUi(lockRemainingMs = vm.lockRemainingMs())) }
    val remaining = rememberLockCountdown(ui.lockRemainingMs) { vm.lockRemainingMs() }
    GuardLoginContent(
        ui = ui.copy(lockRemainingMs = remaining),
        title = "Acceso de seguridad",
        subtitle = "Ingresa la contraseña del personal de guardia",
        buttonText = "Entrar al panel",
        onPasswordChange = { ui = ui.copy(password = it, error = null) },
        onSubmit = {
            val r = vm.login(ui.password.toCharArray())
            if (r is AuthOutcome.Success) onSuccess()
            else {
                val (err, lock) = r.toError()
                ui = ui.copy(password = "", error = err, lockRemainingMs = lock)
            }
        },
        onBack = onBack
    )
}

@Composable
fun ReauthScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    var ui by remember { mutableStateOf(LoginUi(lockRemainingMs = vm.lockRemainingMs())) }
    val remaining = rememberLockCountdown(ui.lockRemainingMs) { vm.lockRemainingMs() }
    GuardLoginContent(
        ui = ui.copy(lockRemainingMs = remaining),
        title = "Salir del modo kiosco",
        subtitle = "Por seguridad, confirma la contraseña del guardia",
        buttonText = "Confirmar y salir",
        onPasswordChange = { ui = ui.copy(password = it, error = null) },
        onSubmit = {
            val r = vm.reauth(ui.password.toCharArray())
            if (r is AuthOutcome.Success) onSuccess()
            else {
                val (err, lock) = r.toError()
                ui = ui.copy(password = "", error = err, lockRemainingMs = lock)
            }
        },
        onBack = onBack
    )
}

/** Fondo de marca con tarjeta centrada para pantallas de autenticación. */
@Composable
fun AuthScaffold(onBack: (() -> Unit)?, content: @Composable () -> Unit) {
    val ext = AppTheme.extended
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.fillMaxWidth().height(260.dp).background(
                Brush.linearGradient(listOf(BrandConfig.palette.navy, ext.heroGradientStart)),
                RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(Spacing.xs)) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                }
            }
            BrandLogoTile(size = 60.dp)
            Spacer(Modifier.height(Spacing.md))
            Text(BrandConfig.APP_NAME, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Panel de seguridad", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(Spacing.xl))
            Surface(
                modifier = Modifier.padding(horizontal = Spacing.screenCompact).widthIn(max = 460.dp).fillMaxWidth(),
                shape = AppShapes.card,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(Modifier.padding(Spacing.xxl)) { content() }
            }
            Spacer(Modifier.height(Spacing.xl))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Contraseña protegida con PBKDF2 en este dispositivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
fun GuardLoginContent(
    ui: LoginUi,
    title: String,
    subtitle: String,
    buttonText: String,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: (() -> Unit)?
) {
    AuthScaffold(onBack) {
        AnimatedContent(targetState = ui.locked, label = "lock") { locked ->
            if (locked) {
                LockoutPanel(ui.lockRemainingMs)
            } else {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleLarge)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(Spacing.xl))
                    PasswordField(
                        value = ui.password,
                        onValueChange = onPasswordChange,
                        label = "Contraseña",
                        isError = ui.error != null,
                        onImeAction = { if (ui.password.isNotEmpty()) onSubmit() }
                    )
                    if (ui.error != null) {
                        Spacer(Modifier.height(Spacing.sm))
                        AlertBanner(title = ui.error, tone = Tone.Danger, icon = Icons.Rounded.Warning)
                    }
                    Spacer(Modifier.height(Spacing.xl))
                    PrimaryButton(
                        buttonText, onClick = onSubmit, enabled = ui.password.isNotEmpty(),
                        icon = Icons.AutoMirrored.Rounded.Login, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "Tras ${GuardAuthManager.MAX_ATTEMPTS} intentos fallidos el acceso se bloquea ${GuardAuthManager.LOCKOUT_MS / 60_000} minutos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LockoutPanel(remainingMs: Long) {
    val ext = AppTheme.extended
    val total = GuardAuthManager.LOCKOUT_MS.toFloat()
    val fraction by animateFloatAsState((remainingMs / total).coerceIn(0f, 1f), tween(900), label = "lockRing")
    val totalSec = (remainingMs + 999) / 1000
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val sw = 12.dp.toPx()
                val inset = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2)
                val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                drawArc(track, 0f, 360f, false, inset, sz, style = Stroke(sw))
                drawArc(ext.danger, -90f, 360f * fraction, false, inset, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.LockClock, contentDescription = null, tint = ext.danger)
                Text("%d:%02d".format(totalSec / 60, totalSec % 60), style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Text("Acceso bloqueado temporalmente", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Se detectaron ${GuardAuthManager.MAX_ATTEMPTS} intentos fallidos. Podrás intentarlo de nuevo cuando termine la cuenta regresiva.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FirstPasswordSetupScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("Guardia ${BrandConfig.INSTITUTION_SHORT}") }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AuthScaffold(onBack) {
        Text("Configura el acceso", style = MaterialTheme.typography.titleLarge)
        Text(
            "Primera vez: define el nombre del guardia y una contraseña segura.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xl))
        AppTextField(name, { name = it }, "Nombre del guardia o caseta", leadingIcon = Icons.Rounded.Badge)
        Spacer(Modifier.height(Spacing.sm))
        PasswordField(p1, { p1 = it; error = null }, "Nueva contraseña", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(Spacing.sm))
        PasswordStrengthMeter(p1)
        Spacer(Modifier.height(Spacing.sm))
        PasswordField(
            p2, { p2 = it; error = null }, "Confirmar contraseña",
            isError = p2.isNotEmpty() && p2 != p1,
            supportingText = if (p2.isNotEmpty() && p2 != p1) "Las contraseñas no coinciden" else "Mínimo 6 caracteres"
        )
        error?.let {
            Spacer(Modifier.height(Spacing.sm))
            AlertBanner(title = it, tone = Tone.Danger, icon = Icons.Rounded.Warning)
        }
        Spacer(Modifier.height(Spacing.xl))
        PrimaryButton("Guardar y entrar", modifier = Modifier.fillMaxWidth(), enabled = p1.isNotEmpty() && p2.isNotEmpty(), onClick = {
            if (p1 != p2) { error = "Las contraseñas no coinciden"; return@PrimaryButton }
            when (val r = vm.setup(p1.toCharArray(), name.trim().ifBlank { "Guardia" })) {
                is AuthOutcome.Success -> onDone()
                is AuthOutcome.Error -> error = r.message
                is AuthOutcome.Locked -> error = "Bloqueado"
            }
        })
    }
}

@Composable
fun ChangePasswordScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    var cur by remember { mutableStateOf("") }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    LaunchedEffect(success) {
        if (success) {
            delay(1_200)
            onDone()
        }
    }
    AuthScaffold(onBack) {
        Text("Cambiar contraseña", style = MaterialTheme.typography.titleLarge)
        Text(
            "Guardia: ${vm.guardName()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xl))
        PasswordField(cur, { cur = it; error = null }, "Contraseña actual", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(Spacing.sm))
        PasswordField(p1, { p1 = it; error = null }, "Nueva contraseña", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(Spacing.sm))
        PasswordStrengthMeter(p1)
        Spacer(Modifier.height(Spacing.sm))
        PasswordField(
            p2, { p2 = it; error = null }, "Confirmar nueva contraseña",
            isError = p2.isNotEmpty() && p2 != p1,
            supportingText = if (p2.isNotEmpty() && p2 != p1) "Las contraseñas no coinciden" else null
        )
        error?.let {
            Spacer(Modifier.height(Spacing.sm))
            AlertBanner(title = it, tone = Tone.Danger, icon = Icons.Rounded.Warning)
        }
        if (success) {
            Spacer(Modifier.height(Spacing.sm))
            AlertBanner(title = "Contraseña actualizada", tone = Tone.Success, icon = Icons.Rounded.Shield)
        }
        Spacer(Modifier.height(Spacing.xl))
        PrimaryButton("Actualizar contraseña", modifier = Modifier.fillMaxWidth(), enabled = cur.isNotEmpty() && p1.isNotEmpty(), onClick = {
            if (p1 != p2) { error = "Las contraseñas no coinciden"; return@PrimaryButton }
            when (val r = vm.change(cur.toCharArray(), p1.toCharArray())) {
                is AuthOutcome.Success -> { success = true; cur = ""; p1 = ""; p2 = "" }
                is AuthOutcome.Error -> error = r.message
                is AuthOutcome.Locked -> error = "Bloqueado ${(r.remainingMs / 1000)} s por intentos fallidos"
            }
        })
    }
}
