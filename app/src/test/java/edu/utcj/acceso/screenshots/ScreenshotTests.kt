package edu.utcj.acceso.screenshots

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import edu.utcj.acceso.analytics.TestTime
import edu.utcj.acceso.data.repository.SyncStatusLabel
import edu.utcj.acceso.data.repository.SyncUiState
import edu.utcj.acceso.domain.analytics.AlertsEngine
import edu.utcj.acceso.domain.analytics.DashboardCalculator
import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.admin.AccessLogContent
import edu.utcj.acceso.ui.admin.AccessLogUi
import edu.utcj.acceso.ui.admin.AdminActions
import edu.utcj.acceso.ui.admin.AdminSection
import edu.utcj.acceso.ui.admin.AdminShellContent
import edu.utcj.acceso.ui.admin.DashboardContent
import edu.utcj.acceso.ui.admin.DashboardUi
import edu.utcj.acceso.ui.admin.LogRange
import edu.utcj.acceso.ui.admin.SettingsContent
import edu.utcj.acceso.ui.admin.SettingsUi
import edu.utcj.acceso.ui.admin.StudentFilter
import edu.utcj.acceso.ui.admin.StudentsContent
import edu.utcj.acceso.ui.admin.StudentsUi
import edu.utcj.acceso.ui.admin.filterStudents
import edu.utcj.acceso.ui.components.CameraPlaceholder
import edu.utcj.acceso.ui.guard.GuardLoginContent
import edu.utcj.acceso.ui.guard.LoginUi
import edu.utcj.acceso.ui.kiosk.KioskContent
import edu.utcj.acceso.ui.kiosk.KioskViewModel
import edu.utcj.acceso.ui.onboarding.OnboardingContent
import edu.utcj.acceso.ui.role.RoleSelectScreen
import edu.utcj.acceso.ui.student.QrRenderer
import edu.utcj.acceso.ui.student.QrUi
import edu.utcj.acceso.ui.student.RegStep
import edu.utcj.acceso.ui.student.RegistrationContent
import edu.utcj.acceso.ui.student.RegistrationUi
import edu.utcj.acceso.ui.student.StudentHomeContent
import edu.utcj.acceso.ui.student.StudentQrTab
import edu.utcj.acceso.ui.scan.PanelScanResult
import edu.utcj.acceso.ui.scan.ScanCardModel
import edu.utcj.acceso.ui.scan.ScanContent
import edu.utcj.acceso.ui.scan.ScanUi
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.domain.qr.RegistrationPayload
import edu.utcj.acceso.ui.theme.AccesoUtcjTheme
import edu.utcj.acceso.util.AppWindowSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de pantalla con datos ficticios (Roborazzi + Robolectric, sin emulador).
 *
 * - `./gradlew testDebugUnitTest` solo renderiza (prueba de humo, no escribe PNG).
 * - `./gradlew recordRoborazziDebug` escribe los PNG en `docs/screenshots/`.
 *
 * Se usa movimiento reducido para que las animaciones infinitas (línea de escaneo, shimmer)
 * no impidan que la UI quede en reposo.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PHONE)
class ScreenshotTests {

    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AccesoUtcjTheme(darkTheme = dark, reducedMotion = true) { content() }
        }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("$OUT/$name.png")
    }

    @Test
    fun s01_onboarding() = shot("01-onboarding") {
        OnboardingContent(pagerState = rememberPagerState { 3 }, onFinish = {})
    }

    @Test
    fun s02_roleSelection() = shot("02-seleccion-de-rol") {
        RoleSelectScreen(onStudent = {}, onStudentAccess = {}, onGuard = {})
    }

    @Test
    fun s02b_roleSelectionDark() = shot("02b-seleccion-de-rol-oscuro", dark = true) {
        RoleSelectScreen(onStudent = {}, onStudentAccess = {}, onGuard = {})
    }

    @Test
    fun s03_registrationConsent() = shot("03-registro-consentimiento") {
        RegistrationContent(
            state = RegistrationUi(
                step = RegStep.CONSENT,
                matricula = "20231045",
                nombre = "Ana Sofía López",
                carrera = "Ingeniería en Software · 5A",
                consentAccepted = true
            ),
            onMatricula = {}, onNombre = {}, onCarrera = {}, onCorreo = {}, onConsent = {}, onNext = {}, onBack = {},
            onGoHome = {}, onFinish = {}, onDeleteData = {}
        )
    }

    @Test
    fun s03b_registrationData() = shot("03b-registro-datos") {
        RegistrationContent(
            state = RegistrationUi(
                step = RegStep.DATA, matricula = "20231045", nombre = "Ana Sofía López",
                carrera = "Ingeniería en Software · 5A", correo = "ana.lopez@utcj.edu.mx"
            ),
            onMatricula = {}, onNombre = {}, onCarrera = {}, onCorreo = {}, onConsent = {}, onNext = {}, onBack = {},
            onGoHome = {}, onFinish = {}, onDeleteData = {}
        )
    }

    private val ana = Student(
        "20231045", "Ana Sofía López", "Ingeniería en Software · 5A", StudentStatus.PENDING,
        consentVersion = "2.0.0", consentTimestampMs = TestTime.at(2026, 9, 1, 9),
        createdAtMs = TestTime.at(2026, 9, 1, 9), correo = "ana.lopez@utcj.edu.mx", hasQrKey = true
    )

    @Test
    fun s04_studentAccessQr() = shot("04-alumno-qr-acceso") {
        StudentHomeContent(
            matricula = "20231045",
            student = ana,
            hasKey = true,
            tab = StudentQrTab.ACCESS,
            registrationQr = null,
            accessQr = QrUi(QrRenderer.render(DEMO_ACCESS_QR).asImageBitmap(), 42, 60),
            validitySec = 60,
            onTab = {}, onMarkApproved = {}, onValidity = {},
            onBack = {}, onReRegister = {}, onDeleteData = {}, onSignOut = {}
        )
    }

    @Test
    fun s04b_studentRegistrationQr() = shot("04b-alumno-qr-registro") {
        StudentHomeContent(
            matricula = "20231045",
            student = ana,
            hasKey = true,
            tab = StudentQrTab.REGISTRATION,
            registrationQr = QrRenderer.render(DEMO_REGISTRATION_QR).asImageBitmap(),
            accessQr = null,
            validitySec = 60,
            onTab = {}, onMarkApproved = {}, onValidity = {},
            onBack = {}, onReRegister = {}, onDeleteData = {}, onSignOut = {}
        )
    }

    @Test
    @Config(qualifiers = KIOSK)
    fun s05_kioskIdle() = shot("05-kiosco-espera") {
        KioskContent(
            state = KioskViewModel.Ui(),
            nowMs = TestTime.at(2026, 10, 8, 7, 42),
            sync = SyncUiState(SyncStatusLabel.ONLINE, 0, "En línea"),
            secondsLeft = 10, totalSeconds = 10,
            onCallGuard = {}, onDismissResult = {}, onExit = {},
            cameraContent = { CameraPlaceholder() }
        )
    }

    @Test
    @Config(qualifiers = KIOSK)
    fun s06_kioskGranted() = shot("06-kiosco-acceso-permitido") {
        KioskContent(
            state = KioskViewModel.Ui(result = allowedCard),
            nowMs = TestTime.at(2026, 10, 8, 7, 42),
            sync = SyncUiState(SyncStatusLabel.ONLINE, 0, "En línea"),
            secondsLeft = 7, totalSeconds = 10,
            onCallGuard = {}, onDismissResult = {}, onExit = {},
            cameraContent = { CameraPlaceholder() }
        )
    }

    @Test
    @Config(qualifiers = KIOSK)
    fun s07_kioskDenied() = shot("07-kiosco-acceso-denegado") {
        KioskContent(
            state = KioskViewModel.Ui(result = deniedCard),
            nowMs = TestTime.at(2026, 10, 8, 7, 44),
            sync = SyncUiState(SyncStatusLabel.PENDING, 3, "Pendientes: 3"),
            secondsLeft = 9, totalSeconds = 10,
            onCallGuard = {}, onDismissResult = {}, onExit = {},
            cameraContent = { CameraPlaceholder() }
        )
    }

    @Test
    fun s08_guardLogin() = shot("08-guardia-inicio-de-sesion") {
        GuardLoginContent(
            ui = LoginUi(password = "caseta2026"),
            title = "Acceso de seguridad",
            subtitle = "Ingresa la contraseña del personal de guardia",
            buttonText = "Entrar al panel",
            onPasswordChange = {}, onSubmit = {}, onBack = {}
        )
    }

    @Test
    fun s08b_guardLockout() = shot("08b-guardia-bloqueo") {
        GuardLoginContent(
            ui = LoginUi(lockRemainingMs = 192_000),
            title = "Acceso de seguridad",
            subtitle = "Ingresa la contraseña del personal de guardia",
            buttonText = "Entrar al panel",
            onPasswordChange = {}, onSubmit = {}, onBack = {}
        )
    }

    @Test
    fun s09_adminDashboardPhone() = shot("09-panel-inicio-telefono") {
        AdminFrame(AdminSection.DASHBOARD, AppWindowSize.Compact) { DashboardBody() }
    }

    @Test
    fun s09c_adminDashboardDark() = shot("09c-panel-inicio-oscuro", dark = true) {
        AdminFrame(AdminSection.DASHBOARD, AppWindowSize.Compact) { DashboardBody() }
    }

    @Test
    @Config(qualifiers = TABLET)
    fun s09b_adminDashboardTablet() = shot("09b-panel-inicio-tableta") {
        AdminFrame(AdminSection.DASHBOARD, AppWindowSize.Expanded) { DashboardBody() }
    }

    @Test
    fun s10_students() = shot("10-panel-alumnos") {
        AdminFrame(AdminSection.STUDENTS, AppWindowSize.Compact) {
            StudentsContent(
                ui = StudentsUi(
                    loading = false,
                    students = filterStudents(FakeData.students, "", StudentFilter.ALL),
                    counts = StudentFilter.entries.associateWith { f -> FakeData.students.count { f.matches(it.status) } }
                ),
                onQuery = {}, onFilter = {}, onApprove = {}, onReject = {}, onDelete = {}, onManualEntry = {}
            )
        }
    }

    @Test
    fun s11_accessLog() = shot("11-panel-bitacora") {
        AdminFrame(AdminSection.LOG, AppWindowSize.Compact) {
            AccessLogContent(
                ui = AccessLogUi(
                    loading = false,
                    range = LogRange.Week,
                    events = FakeData.events.sortedByDescending { it.datetimeMs }.take(30),
                    nowMs = FakeData.NOW
                ),
                onQuery = {}, onRange = {}, onResult = {}, onExportCsv = {}, onExportPdf = {}
            )
        }
    }

    @Test
    fun s12_settings() = shot("12-panel-configuracion") {
        AdminFrame(AdminSection.SETTINGS, AppWindowSize.Compact) {
            SettingsContent(
                ui = SettingsUi(maxQrValiditySec = 120),
                sync = SyncUiState(SyncStatusLabel.ONLINE, 0, "En línea"),
                onChange = {}, onImportCsv = {}, onLoadSample = {}, onSyncNow = {},
                onChangePassword = {}, onLogout = {}, onEvalMode = {}, onKiosk = {}
            )
        }
    }

    private val allowedCard = ScanCardModel(
        allowed = true, nombre = "Ana Sofía López", matricula = "20231045",
        carrera = "Ingeniería en Software · 5A", status = StudentStatus.APPROVED,
        timeMs = TestTime.at(2026, 10, 8, 7, 42)
    )
    private val deniedCard = ScanCardModel(
        allowed = false, nombre = "Luis Ramírez Torres", matricula = "20220311",
        carrera = "Redes y Telecomunicaciones", status = StudentStatus.BAJA,
        reasonTitle = DenialReason.BAJA.labelEs, reasonDetail = DenialReason.BAJA.detailEs,
        timeMs = TestTime.at(2026, 10, 8, 7, 44)
    )

    private fun scan(ui: ScanUi): @Composable () -> Unit = {
        AdminFrame(AdminSection.SCAN, AppWindowSize.Compact) {
            ScanContent(
                ui = ui, onRegisterEntry = {}, onDeny = {}, onApprove = {}, onReject = {}, onDismiss = {},
                onManualEntry = {}, cameraContent = { CameraPlaceholder() }
            )
        }
    }

    @Test
    fun s13_guardScanner() = shot("13-guardia-escaner", content = scan(ScanUi()))

    @Test
    fun s14_guardScanAllowed() = shot("14-guardia-acceso-permitido", content = scan(
        ScanUi(result = PanelScanResult.Access(allowedCard, awaitingGuard = true))
    ))

    @Test
    fun s15_guardScanDenied() = shot("15-guardia-acceso-denegado", content = scan(
        ScanUi(
            result = PanelScanResult.Access(
                ScanCardModel(
                    allowed = false, nombre = "María Fernanda Ruiz", matricula = "20240077",
                    carrera = "Negocios Internacionales", status = StudentStatus.APPROVED,
                    reasonTitle = DenialReason.EXPIRED.labelEs, reasonDetail = DenialReason.EXPIRED.detailEs,
                    timeMs = TestTime.at(2026, 10, 8, 7, 51)
                ),
                awaitingGuard = false
            )
        )
    ))

    @Test
    fun s16_guardApproveRegistration() = shot("16-guardia-aprobar-registro", content = scan(
        ScanUi(
            result = PanelScanResult.Registration(
                payload = RegistrationPayload(
                    "20241203", "Jorge Alberto Núñez", "Manufactura · 2B", "jorge.nunez@utcj.edu.mx",
                    "2.0.0", ByteArray(91), TestTime.at(2026, 10, 8, 7, 30)
                ),
                kind = PanelScanResult.Registration.Kind.READY,
                previousStatus = StudentStatus.ACTIVO
            )
        )
    ))

    @Composable
    private fun AdminFrame(section: AdminSection, size: AppWindowSize, body: @Composable () -> Unit) {
        AdminShellContent(
            section = section,
            windowSize = size,
            pendingCount = 2,
            guardName = "Carlos Méndez",
            sync = SyncUiState(SyncStatusLabel.ONLINE, 0, "En línea"),
            actions = AdminActions()
        ) { body() }
    }

    @Composable
    private fun DashboardBody() {
        val events = FakeData.events
        val todayStart = edu.utcj.acceso.util.TimeUtil.startOfDayMs(FakeData.NOW)
        DashboardContent(
            ui = DashboardUi(
                loading = false,
                stats = DashboardCalculator.compute(events, FakeData.NOW),
                alerts = AlertsEngine.build(events.filter { it.datetimeMs >= todayStart }, 6, 22),
                pendingCount = 2,
                nowMs = FakeData.NOW
            ),
            guardName = "Carlos Méndez",
            onKiosk = {}, onManualEntry = {}, onOpen = {}
        )
    }
}

const val OUT = "../docs/screenshots"

/** Textos fijos para que los QR de las capturas no cambien entre ejecuciones. */
const val DEMO_ACCESS_QR = "UTCJA1.MjAyMzEwNDU.1791476520.1791476580.q0N2aW1wbGVub25jZQ.MEUCIQDdemoSignatureForScreenshotsOnly0000000000000000AiB"
const val DEMO_REGISTRATION_QR = "UTCJR1.MjAyMzEwNDU.QW5hIFNvZsOtYSBMw7NwZXo.SW5nZW5pZXLDrWEgZW4gU29mdHdhcmU.YW5hLmxvcGV6QHV0Y2ouZWR1Lm14.Mi4wLjA." +
    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEdemoPublicKeyForScreenshotsOnly000000000000000000000000000000000000000000000000.1788274800.MEUCIQDdemo"
const val PHONE = "w393dp-h852dp-port-xxhdpi"
const val TABLET = "w1280dp-h800dp-land-xhdpi"
const val KIOSK = "w1280dp-h800dp-land-xhdpi"

/** Datos ficticios deterministas para las capturas. */
object FakeData {
    val NOW = TestTime.at(2026, 10, 8, 13, 20)

    val students = listOf(
        Student("20231045", "Ana Sofía López", "Ingeniería en Software", StudentStatus.APPROVED, hasQrKey = true),
        Student("20230112", "Diego Hernández", "Mecatrónica", StudentStatus.APPROVED, hasQrKey = true),
        Student("20240077", "María Fernanda Ruiz", "Negocios Internacionales", StudentStatus.PENDING),
        Student("20220311", "Luis Ramírez Torres", "Redes y Telecomunicaciones", StudentStatus.BAJA),
        Student("20230988", "Valeria Castillo", "Ingeniería en Software", StudentStatus.ACTIVO, hasQrKey = true),
        Student("20241203", "Jorge Alberto Núñez", "Manufactura", StudentStatus.PENDING),
        Student("20210544", "Paola Gutiérrez", "Energías Renovables", StudentStatus.SUSPENDIDO),
        Student("20230671", "Ricardo Salinas", "Mantenimiento Industrial", StudentStatus.APPROVED, hasQrKey = true),
        Student("20231999", "Fernanda Ortiz", "Diseño Digital", StudentStatus.REJECTED)
    )

    val events: List<AccessEvent> = buildList {
        var id = 1L
        val names = students.filter { it.status.allowsAccess() }
        // Últimos 6 días: tendencia variable.
        val perDay = listOf(64, 71, 58, 22, 18, 76, 0)
        for (d in 6 downTo 1) {
            val dayStart = edu.utcj.acceso.util.TimeUtil.daysAgoStartMs(d, NOW)
            repeat(perDay[6 - d]) { i ->
                val s = names[i % names.size]
                val t = dayStart + (7 * 60 + (i * 9) % 600) * 60_000L
                add(AccessEvent(id++, t, s.matricula, s.nombre, AccessResult.ALLOWED, AccessMethod.QR, verifyDurationMs = 120))
                if (i % 9 == 0) add(AccessEvent(id++, t + 30_000, s.matricula, s.nombre, AccessResult.DENIED, AccessMethod.QR, reason = "QR vencido"))
            }
        }
        // Hoy: pico a las 7 y 13 h.
        val today = edu.utcj.acceso.util.TimeUtil.startOfDayMs(NOW)
        val hours = listOf(6 to 4, 7 to 21, 8 to 12, 9 to 6, 10 to 5, 11 to 4, 12 to 7, 13 to 9)
        hours.forEach { (h, n) ->
            repeat(n) { i ->
                val s = names[(h + i) % names.size]
                val t = today + h * 3_600_000L + i * 150_000L
                val method = if (i % 11 == 7) AccessMethod.MANUAL else AccessMethod.QR
                val result = if (method == AccessMethod.MANUAL) AccessResult.MANUAL else AccessResult.ALLOWED
                add(
                    AccessEvent(
                        id++, t, s.matricula, s.nombre, result, method,
                        authorizingGuard = if (method == AccessMethod.MANUAL) "Carlos Méndez" else "Caseta Norte",
                        reason = if (method == AccessMethod.MANUAL) "Credencial física verificada" else null,
                        verifyDurationMs = if (method == AccessMethod.QR) 90L + (i * 37) % 120 else null
                    )
                )
            }
        }
        add(AccessEvent(id++, today + 7 * 3_600_000L + 600_000, "20220311", "Luis Ramírez Torres", AccessResult.DENIED, AccessMethod.QR, reason = "Alumno dado de baja"))
        repeat(3) { add(AccessEvent(id++, today + 13 * 3_600_000L + 900_000 + it * 40_000L, "20210544", "Paola Gutiérrez", AccessResult.DENIED, AccessMethod.QR, reason = "Alumno suspendido")) }
        add(AccessEvent(id++, today + 12 * 3_600_000L + 300_000, "20240077", "María Fernanda Ruiz", AccessResult.DENIED, AccessMethod.QR, reason = "Pendiente de aprobación"))
    }
}
