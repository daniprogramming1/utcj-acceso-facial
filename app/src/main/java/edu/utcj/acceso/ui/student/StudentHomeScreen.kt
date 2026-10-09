package edu.utcj.acceso.ui.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.ConsentRecord
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.QrCrypto
import edu.utcj.acceso.domain.qr.QrKeyStore
import edu.utcj.acceso.domain.qr.QrSigner
import edu.utcj.acceso.ui.components.AccesoTopBar
import edu.utcj.acceso.ui.components.AlertBanner
import edu.utcj.acceso.ui.components.AppCard
import edu.utcj.acceso.ui.components.ConfirmDialog
import edu.utcj.acceso.ui.components.DetailRow
import edu.utcj.acceso.ui.components.InitialsAvatar
import edu.utcj.acceso.ui.components.PrimaryButton
import edu.utcj.acceso.ui.components.SecondaryButton
import edu.utcj.acceso.ui.components.SectionHeader
import edu.utcj.acceso.ui.components.StatusPill
import edu.utcj.acceso.ui.components.Tone
import edu.utcj.acceso.ui.components.TonalButton
import edu.utcj.acceso.ui.components.labelEs
import edu.utcj.acceso.ui.components.screenHorizontalPadding
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Spacing
import edu.utcj.acceso.util.MaxBrightnessEffect
import edu.utcj.acceso.util.TimeUtil
import edu.utcj.acceso.util.isCompactWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** QR de acceso vigente con su cuenta regresiva. */
data class QrUi(val image: ImageBitmap, val secondsLeft: Int, val totalSeconds: Int)

/** Pestañas del inicio del alumno. */
enum class StudentQrTab(val label: String) { ACCESS("QR de acceso"), REGISTRATION("QR de registro") }

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val students: StudentRepository,
    private val settings: SettingsRepository,
    private val keys: QrKeyStore
) : ViewModel() {
    val matricula: String? = settings.getRememberedStudent()

    val student: StateFlow<Student?> = (matricula?.let { students.observe(it) } ?: flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _hasKey = MutableStateFlow(true)
    val hasKey: StateFlow<Boolean> = _hasKey.asStateFlow()

    private val _tab = MutableStateFlow(if (settings.isStudentMarkedApproved()) StudentQrTab.ACCESS else StudentQrTab.REGISTRATION)
    val tab: StateFlow<StudentQrTab> = _tab.asStateFlow()

    private val _validity = MutableStateFlow(settings.getStudentQrValiditySec())
    val validity: StateFlow<Int> = _validity.asStateFlow()

    private val _registrationQr = MutableStateFlow<ImageBitmap?>(null)
    val registrationQr: StateFlow<ImageBitmap?> = _registrationQr.asStateFlow()

    private val _accessQr = MutableStateFlow<QrUi?>(null)
    val accessQr: StateFlow<QrUi?> = _accessQr.asStateFlow()

    private var signer: QrSigner? = null
    private var accessJob: Job? = null

    init {
        viewModelScope.launch {
            val mat = matricula ?: return@launch
            signer = withContext(Dispatchers.Default) { runCatching { keys.get(mat) }.getOrNull() }
            _hasKey.value = signer != null
            val s = students.get(mat)
            // Mismo equipo que el guardia (demo) o aprobado: abrir directo en el QR de acceso.
            if (s?.status?.allowsAccess() == true) _tab.value = StudentQrTab.ACCESS
            buildRegistrationQr(s)
        }
    }

    private suspend fun buildRegistrationQr(s: Student?) {
        val sg = signer ?: return
        val st = s ?: return
        _registrationQr.value = withContext(Dispatchers.Default) {
            val ts = st.consentTimestampMs.takeIf { it > 0 } ?: System.currentTimeMillis()
            val raw = QrCrypto.registrationQr(
                sg, st.matricula, st.nombre, st.carrera, st.correo,
                st.consentVersion.ifBlank { ConsentRecord.CURRENT_VERSION }, ts
            )
            QrRenderer.render(raw).asImageBitmap()
        }
    }

    fun selectTab(t: StudentQrTab) { _tab.value = t }

    fun markApproved() {
        settings.setStudentMarkedApproved(true)
        _tab.value = StudentQrTab.ACCESS
    }

    fun setValidity(sec: Int) {
        settings.setStudentQrValiditySec(sec)
        _validity.value = settings.getStudentQrValiditySec()
        if (accessJob?.isActive == true) startAccessQr()
    }

    /** Genera el QR de acceso y lo renueva solo al vencer. */
    fun startAccessQr() {
        val mat = matricula ?: return
        val sg = signer ?: return
        accessJob?.cancel()
        accessJob = viewModelScope.launch {
            while (isActive) {
                val total = _validity.value
                val now = System.currentTimeMillis()
                val image = withContext(Dispatchers.Default) {
                    QrRenderer.render(QrCrypto.accessQr(sg, mat, total, now)).asImageBitmap()
                }
                val expires = now / 1000 * 1000 + total * 1000L
                while (isActive) {
                    val left = ((expires - System.currentTimeMillis() + 999) / 1000).toInt()
                    if (left <= 0) break
                    _accessQr.value = QrUi(image, left, total)
                    delay(250)
                }
            }
        }
    }

    fun stopAccessQr() {
        accessJob?.cancel()
        _accessQr.value = null
    }

    fun signOut() {
        stopAccessQr()
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
    val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val validity by vm.validity.collectAsStateWithLifecycle()
    val regQr by vm.registrationQr.collectAsStateWithLifecycle()
    val accessQr by vm.accessQr.collectAsStateWithLifecycle()
    // El QR de acceso solo se genera mientras la pestaña está visible.
    DisposableEffect(tab, hasKey) {
        if (tab == StudentQrTab.ACCESS && hasKey) vm.startAccessQr() else vm.stopAccessQr()
        onDispose { vm.stopAccessQr() }
    }
    MaxBrightnessEffect(enabled = hasKey && (accessQr != null || regQr != null))
    StudentHomeContent(
        matricula = vm.matricula,
        student = student,
        hasKey = hasKey,
        tab = tab,
        registrationQr = regQr,
        accessQr = accessQr,
        validitySec = validity,
        onTab = vm::selectTab,
        onMarkApproved = vm::markApproved,
        onValidity = vm::setValidity,
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
    hasKey: Boolean,
    tab: StudentQrTab,
    registrationQr: ImageBitmap?,
    accessQr: QrUi?,
    validitySec: Int,
    onTab: (StudentQrTab) -> Unit,
    onMarkApproved: () -> Unit,
    onValidity: (Int) -> Unit,
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
            val qrCard: @Composable (Modifier) -> Unit = { m ->
                if (!hasKey) MissingKeyCard(onReRegister, m)
                else QrCard(student, tab, registrationQr, accessQr, validitySec, onTab, onMarkApproved, onValidity, m)
            }
            if (compact) {
                Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    qrCard(Modifier)
                    DetailsCard(student, onReRegister, onDeleteData)
                }
            } else {
                Row(Modifier.widthIn(max = 1100.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    qrCard(Modifier.weight(1f))
                    Column(Modifier.weight(1f)) { DetailsCard(student, onReRegister, onDeleteData) }
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
    if (confirmSignOut) {
        ConfirmDialog(
            title = "¿Salir de este dispositivo?",
            message = "Dejarás de ver tu QR en esta pantalla. Tu registro y tu llave se conservan; vuelve con «Ya tengo registro».",
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
    }
}

@Composable
private fun MissingKeyCard(onReRegister: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        AlertBanner(
            title = "Este teléfono no tiene tu llave segura",
            message = "Puede pasar si borraste los datos de la app o cambiaste de teléfono. Regístrate de nuevo y muestra tu nuevo QR de registro al guardia.",
            tone = Tone.Warning,
            icon = Icons.Rounded.Key
        )
        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton("Registrarme de nuevo", onClick = onReRegister, icon = Icons.Rounded.Refresh, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun QrCard(
    student: Student?,
    tab: StudentQrTab,
    registrationQr: ImageBitmap?,
    accessQr: QrUi?,
    validitySec: Int,
    onTab: (StudentQrTab) -> Unit,
    onMarkApproved: () -> Unit,
    onValidity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val blocked = student?.status == StudentStatus.BAJA || student?.status == StudentStatus.SUSPENDIDO
    AppCard(modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        TabRow(selectedTabIndex = tab.ordinal, containerColor = Color.Transparent) {
            StudentQrTab.entries.forEach { t ->
                Tab(selected = tab == t, onClick = { onTab(t) }, text = { Text(t.label, style = MaterialTheme.typography.titleSmall) })
            }
        }
        Column(Modifier.padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            if (blocked) {
                AlertBanner(
                    title = "Acceso bloqueado (${student?.status?.labelEs()})",
                    message = "Tu estatus institucional no permite el acceso. Acude a Servicios Escolares.",
                    tone = Tone.Danger,
                    icon = Icons.Rounded.Warning,
                    modifier = Modifier.padding(bottom = Spacing.lg)
                )
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "qrTab"
            ) { t ->
                when (t) {
                    StudentQrTab.ACCESS -> AccessQrPane(accessQr, validitySec, onValidity)
                    StudentQrTab.REGISTRATION -> RegistrationQrPane(student, registrationQr, onMarkApproved)
                }
            }
        }
    }
}

@Composable
private fun QrImage(image: ImageBitmap?, description: String) {
    Box(
        Modifier.fillMaxWidth(0.86f).widthIn(max = 320.dp).aspectRatio(1f)
            .background(Color.White, MaterialTheme.shapes.large)
            .padding(Spacing.md)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (image != null) Image(image, contentDescription = null, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(96.dp))
    }
}

@Composable
private fun AccessQrPane(qr: QrUi?, validitySec: Int, onValidity: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        QrImage(qr?.image, "Código QR de acceso" + (qr?.let { ", vence en ${it.secondsLeft} segundos" } ?: ""))
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CountdownRing(qr?.secondsLeft ?: validitySec, qr?.totalSeconds ?: validitySec, Modifier.size(56.dp))
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    if (qr == null) "Generando tu QR…" else "Se renueva en ${formatLeft(qr.secondsLeft)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Muéstralo al guardia o a la cámara del kiosco",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        HorizontalDivider(color = AppTheme.extended.cardBorder)
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("Vigencia del QR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SettingsRepository.STUDENT_QR_VALIDITY_OPTIONS.forEach { sec ->
                FilterChip(
                    selected = sec == validitySec,
                    onClick = { onValidity(sec) },
                    label = { Text(SettingsRepository.validityLabel(sec)) }
                )
            }
        }
        Text(
            "Cada QR sirve una sola vez y deja de funcionar al vencer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RegistrationQrPane(student: Student?, image: ImageBitmap?, onMarkApproved: () -> Unit) {
    val approvedHere = student?.status?.allowsAccess() == true
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusPill(
            if (approvedHere) "Acceso activo" else "Pendiente de activación",
            if (approvedHere) Tone.Success else Tone.Warning,
            icon = if (approvedHere) Icons.Rounded.CheckCircle else Icons.Rounded.HowToReg
        )
        Spacer(Modifier.height(Spacing.md))
        QrImage(image, "Código QR de registro")
        Spacer(Modifier.height(Spacing.lg))
        Text(
            if (approvedHere) "Tu acceso ya está activo." else "Pendiente: muéstrale este QR al guardia para activar tu acceso",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Solo se muestra una vez, en caseta. Contiene tus datos y tu llave pública; no permite entrar por sí solo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton("Ya me aprobaron", onClick = onMarkApproved, icon = Icons.Rounded.QrCode2, modifier = Modifier.fillMaxWidth())
    }
}

private fun formatLeft(sec: Int): String = if (sec < 60) "$sec s" else "%d:%02d min".format(sec / 60, sec % 60)

/** Anillo de cuenta regresiva con el tiempo restante al centro. */
@Composable
fun CountdownRing(seconds: Int, total: Int, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, track: Color = MaterialTheme.colorScheme.surfaceContainerHighest, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    val ext = AppTheme.extended
    val fraction by animateFloatAsState(seconds / total.coerceAtLeast(1).toFloat(), tween(900), label = "ring")
    val ringColor = if (seconds <= 5) ext.warning else color
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.minDimension * 0.1f
            val tl = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2)
            val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
            drawArc(track, 0f, 360f, false, style = Stroke(sw), topLeft = tl, size = sz)
            drawArc(ringColor, -90f, 360f * fraction, false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = tl, size = sz)
        }
        Text(if (seconds >= 100) "${(seconds + 59) / 60}m" else "$seconds", style = MaterialTheme.typography.titleMedium, color = textColor)
    }
}

@Composable
private fun DetailsCard(student: Student?, onReRegister: () -> Unit, onDeleteData: () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        SectionHeader("Mis datos", subtitle = "Solo datos personales · sin fotos ni biometría")
        Spacer(Modifier.height(Spacing.sm))
        DetailRow("Matrícula", student?.matricula ?: "—", icon = Icons.Rounded.Badge)
        HorizontalDivider(color = AppTheme.extended.cardBorder)
        DetailRow("Carrera", student?.carrera?.ifBlank { "—" } ?: "—", icon = Icons.Rounded.School)
        student?.correo?.let {
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            DetailRow("Correo", it, icon = Icons.Rounded.Email)
        }
        HorizontalDivider(color = AppTheme.extended.cardBorder)
        DetailRow(
            "Consentimiento",
            student?.takeIf { it.consentTimestampMs > 0 }?.let { "v${it.consentVersion} · ${TimeUtil.formatShortDate(it.consentTimestampMs)}" } ?: "—",
            icon = Icons.Rounded.Policy
        )
        HorizontalDivider(color = AppTheme.extended.cardBorder)
        DetailRow("Llave segura", "Guardada en este teléfono", icon = Icons.Rounded.Key)
        student?.approvedAtMs?.let {
            HorizontalDivider(color = AppTheme.extended.cardBorder)
            DetailRow("Revisado", TimeUtil.formatShortDate(it), icon = Icons.Rounded.CalendarMonth)
        }
        Spacer(Modifier.height(Spacing.lg))
        TonalButton("Actualizar mis datos", onClick = onReRegister, icon = Icons.Rounded.Refresh, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton("Eliminar mis datos", onClick = onDeleteData, icon = Icons.Rounded.DeleteOutline, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))
        AlertBanner(
            title = "Actualizar tus datos crea una llave nueva",
            message = "Tendrás que mostrar otra vez tu QR de registro al guardia.",
            tone = Tone.Info,
            icon = Icons.Rounded.Refresh
        )
    }
}
