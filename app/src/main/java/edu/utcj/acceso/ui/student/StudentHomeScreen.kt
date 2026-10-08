package edu.utcj.acceso.ui.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.biometric.QrTokenManager
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.ConfirmDialog
import edu.utcj.acceso.ui.components.DetailRow
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.PendingIllustration
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.SectionHeader
import edu.utcj.acceso.ui.components.StudentStatusChip
import edu.utcj.acceso.ui.components.SuccessIllustration
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.components.TonalButton
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.components.screenHorizontalPadding
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.TimeUtil
import edu.utcj.acceso.util.isCompactWidth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** QR vigente con su cuenta regresiva. */
data class QrUi(val image: ImageBitmap, val secondsLeft: Int, val totalSeconds: Int)

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val students: StudentRepository,
    private val settings: SettingsRepository,
    private val qrTokens: QrTokenManager
) : ViewModel() {
    val matricula: String? = settings.getRememberedStudent()

    val student: StateFlow<Student?> = (matricula?.let { students.observe(it) } ?: flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _samples = MutableStateFlow(0)
    val samples: StateFlow<Int> = _samples.asStateFlow()

    private val _qr = MutableStateFlow<QrUi?>(null)
    val qr: StateFlow<QrUi?> = _qr.asStateFlow()
    private var qrJob: Job? = null

    init {
        viewModelScope.launch { matricula?.let { _samples.value = students.sampleCount(it) } }
    }

    fun showQr() {
        val mat = matricula ?: return
        val s = student.value ?: return
        if (!s.status.allowsAccess()) return
        qrJob?.cancel()
        qrJob = viewModelScope.launch {
            val total = QrTokenManager.VALIDITY_SECONDS.toInt()
            while (true) {
                val image = QrRenderer.render(qrTokens.issue(mat)).asImageBitmap()
                for (sec in total downTo 1) {
                    _qr.value = QrUi(image, sec, total)
                    delay(1_000)
                }
            }
        }
    }

    fun hideQr() {
        qrJob?.cancel()
        _qr.value = null
    }

    fun signOut() {
        hideQr()
        settings.setRememberedStudent(null)
    }
}

@Composable
fun StudentHomeScreen(
    onBack: () -> Unit,
    onReRegister: () -> Unit,
    onDeleteData: (String) -> Unit,
    onSignedOut: () -> Unit,
    onNeedAccess: () -> Unit,
    vm: StudentHomeViewModel = hiltViewModel()
) {
    if (vm.matricula == null) {
        LaunchedEffect(Unit) { onNeedAccess() }
        return
    }
    val student by vm.student.collectAsStateWithLifecycle()
    val samples by vm.samples.collectAsStateWithLifecycle()
    val qr by vm.qr.collectAsStateWithLifecycle()
    StudentHomeContent(
        matricula = vm.matricula,
        student = student,
        sampleCount = samples,
        qr = qr,
        onShowQr = vm::showQr,
        onHideQr = vm::hideQr,
        onBack = onBack,
        onReRegister = onReRegister,
        onDeleteData = { onDeleteData(vm.matricula) },
        onSignOut = { vm.signOut(); onSignedOut() }
    )
}

@Composable
fun StudentHomeContent(
    matricula: String,
    student: Student?,
    sampleCount: Int,
    qr: QrUi?,
    onShowQr: () -> Unit,
    onHideQr: () -> Unit,
    onBack: () -> Unit,
    onReRegister: () -> Unit,
    onDeleteData: () -> Unit,
    onSignOut: () -> Unit
) {
    var confirmSignOut by remember { mutableStateOf(false) }
    val compact = isCompactWidth()
    Scaffold(
        topBar = {
            AccesoTopBar(title = "Mi acceso", subtitle = "Alumno", onBack = onBack, actions = {
                IconButton(onClick = { confirmSignOut = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Salir de este dispositivo")
                }
            })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = screenHorizontalPadding(), vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val name = student?.nombre ?: matricula
            ProfileHeader(name, matricula, student)
            Spacer(Modifier.height(Spacing.lg))
            if (compact) {
                Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    StatusCard(student, sampleCount, onReRegister)
                    QrCard(student, qr, onShowQr, onHideQr)
                    DetailsCard(student, sampleCount, onReRegister, onDeleteData)
                }
            } else {
                Row(Modifier.widthIn(max = 1100.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        StatusCard(student, sampleCount, onReRegister)
                        DetailsCard(student, sampleCount, onReRegister, onDeleteData)
                    }
                    QrCard(student, qr, onShowQr, onHideQr, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
    if (confirmSignOut) {
        ConfirmDialog(
            title = "¿Salir de este dispositivo?",
            message = "Se olvidará tu matrícula en este equipo. Tu registro y tus datos no se eliminan.",
            confirmText = "Salir",
            onConfirm = { confirmSignOut = false; onSignOut() },
            onDismiss = { confirmSignOut = false },
            icon = Icons.AutoMirrored.Rounded.Logout
        )
    }
}

@Composable
private fun ProfileHeader(name: String, matricula: String, student: Student?) {
    Row(Modifier.fillMaxWidth().widthIn(max = 1100.dp), verticalAlignment = Alignment.CenterVertically) {
        InitialsAvatar(name, size = 60.dp)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text("Hola, ${name.substringBefore(' ')}", style = MaterialTheme.typography.headlineSmall)
            Text(
                listOfNotNull(matricula, student?.carrera?.takeIf { it.isNotBlank() }).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (student != null) StudentStatusChip(student.status)
    }
}

@Composable
private fun StatusCard(student: Student?, sampleCount: Int, onReRegister: () -> Unit) {
    val status = student?.status
    val (title, body) = when {
        student == null -> "Sin registro" to "No encontramos tu registro en este dispositivo."
        sampleCount == 0 && status != null && status != StudentStatus.PENDING ->
            "Falta tu registro facial" to "Estás en el directorio institucional, pero aún no registras tu rostro."
        status == StudentStatus.PENDING -> "Pendiente de aprobación" to "Seguridad revisará tu registro en breve. Te recomendamos acudir a la caseta si es urgente."
        status == StudentStatus.APPROVED || status == StudentStatus.ACTIVO -> "¡Listo para entrar!" to "El kiosco te reconocerá al mirar la cámara. Ten tu QR a la mano como respaldo."
        status == StudentStatus.REJECTED -> "Registro rechazado" to "Acude con el personal de seguridad para revisar tu caso o vuelve a registrarte."
        else -> "Acceso bloqueado (${status?.labelEs()})" to "Tu estatus institucional no permite el acceso. Acude a Servicios Escolares."
    }
    val ready = status?.allowsAccess() == true && sampleCount > 0
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ready) SuccessIllustration(Modifier.size(84.dp)) else PendingIllustration(Modifier.size(84.dp))
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text("Estatus de acceso", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(Spacing.xs))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (sampleCount == 0 || status == StudentStatus.REJECTED) {
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton("Registrar mi rostro", onClick = onReRegister, icon = Icons.Rounded.Face, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun QrCard(
    student: Student?,
    qr: QrUi?,
    onShowQr: () -> Unit,
    onHideQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eligible = student?.status?.allowsAccess() == true
    AppCard(modifier.fillMaxWidth()) {
        SectionHeader(
            "Mi QR dinámico",
            subtitle = "Respaldo si el reconocimiento facial falla · se renueva cada ${QrTokenManager.VALIDITY_SECONDS} s"
        )
        Spacer(Modifier.height(Spacing.lg))
        AnimatedContent(
            targetState = qr != null,
            transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(150)) },
            label = "qr"
        ) { showing ->
            if (showing && qr != null) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.fillMaxWidth(0.82f).widthIn(max = 300.dp).aspectRatio(1f)
                            .background(Color.White, MaterialTheme.shapes.large)
                            .padding(Spacing.md)
                    ) {
                        Image(qr.image, contentDescription = "Código QR de acceso, vigente ${qr.secondsLeft} segundos", modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CountdownRing(qr.secondsLeft, qr.totalSeconds, Modifier.size(52.dp))
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            Text("Se renueva en ${qr.secondsLeft} s", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Muéstralo a la cámara del kiosco",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    SecondaryButton("Ocultar QR", onClick = onHideQr, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    if (!eligible) {
                        Text(
                            "Disponible cuando tu registro esté aprobado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(Spacing.md))
                    }
                    PrimaryButton(
                        "Mostrar mi QR", onClick = onShowQr, enabled = eligible,
                        icon = Icons.Rounded.QrCode2, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Anillo de cuenta regresiva con el número de segundos al centro. */
@Composable
fun CountdownRing(seconds: Int, total: Int, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, track: Color = MaterialTheme.colorScheme.surfaceContainerHighest, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    val ext = AppTheme.extended
    val fraction by animateFloatAsState(seconds / total.toFloat(), tween(900), label = "ring")
    val ringColor = if (seconds <= 5) ext.warning else color
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.minDimension * 0.1f
            drawArc(track, 0f, 360f, false, style = Stroke(sw), topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2), size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw))
            drawArc(ringColor, -90f, 360f * fraction, false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2), size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw))
        }
        Text("$seconds", style = MaterialTheme.typography.titleMedium, color = textColor)
    }
}

@Composable
private fun DetailsCard(student: Student?, sampleCount: Int, onReRegister: () -> Unit, onDeleteData: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        SectionHeader("Mis datos", subtitle = "Solo se guardan vectores cifrados, nunca fotos")
        Spacer(Modifier.height(Spacing.sm))
        DetailRow("Registro facial", if (sampleCount > 0) "$sampleCount muestras cifradas" else "Sin muestras", icon = Icons.Rounded.EnhancedEncryption)
        HorizontalDivider(color = AppTheme.extended.cardBorder)
        DetailRow(
            "Consentimiento",
            student?.takeIf { it.consentTimestampMs > 0 }?.let { "v${it.consentVersion} · ${TimeUtil.formatShortDate(it.consentTimestampMs)}" } ?: "—",
            icon = Icons.Rounded.Policy
        )
        HorizontalDivider(color = AppTheme.extended.cardBorder)
        DetailRow("Carrera", student?.carrera?.ifBlank { "—" } ?: "—", icon = Icons.Rounded.School)
        student?.approvedAtMs?.let {
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            DetailRow("Revisado", TimeUtil.formatShortDate(it), icon = Icons.Rounded.CalendarMonth)
        }
        Spacer(Modifier.height(Spacing.lg))
        TonalButton("Actualizar registro facial", onClick = onReRegister, icon = Icons.Rounded.Refresh, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton("Eliminar mis datos", onClick = onDeleteData, icon = Icons.Rounded.DeleteOutline, modifier = Modifier.fillMaxWidth())
        if (student?.status == StudentStatus.PENDING) {
            Spacer(Modifier.height(Spacing.md))
            AlertBanner(
                title = "Volver a registrarte reinicia la revisión",
                tone = Tone.Info,
                icon = Icons.Rounded.Refresh
            )
        }
    }
}
