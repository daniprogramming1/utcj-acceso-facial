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
import edu.utcj.acceso.ui.components.FaceGuideStatus
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
 * Se usa movimiento reducido para que las animaciones infinitas (anillo de escaneo, shimmer)
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
    fun s03_registrationCapture() = shot("03-registro-captura-facial") {
        RegistrationContent(
            state = RegistrationUi(
                step = RegStep.CAPTURE,
                matricula = "20231045",
                nombre = "Ana Sofía López",
                carrera = "Ingeniería en Software",
                consentAccepted = true,
                samples = List(2) { FloatArray(4) },
                guidance = "Acércate un poco más",
                faceStatus = FaceGuideStatus.Adjust
            ),
            onMatricula = {}, onNombre = {}, onCarrera = {}, onConsent = {}, onNext = {}, onBack = {},
            onRetake = {}, onGoHome = {}, onFinish = {}, onDeleteData = {},
            cameraContent = { CameraPlaceholder() }
        )
    }

    @Test
    fun s03b_registrationData() = shot("03b-registro-datos") {
        RegistrationContent(
            state = RegistrationUi(step = RegStep.DATA, matricula = "20231045", nombre = "Ana Sofía López", carrera = "Ingeniería en Software"),
            onMatricula = {}, onNombre = {}, onCarrera = {}, onConsent = {}, onNext = {}, onBack = {},
            onRetake = {}, onGoHome = {}, onFinish = {}, onDeleteData = {},
            cameraContent = { CameraPlaceholder() }
        )
    }

    @Test
    fun s04_studentHomeQr() = shot("04-alumno-inicio-qr") {
        StudentHomeContent(
            matricula = "20231045",
            student = Student(
                "20231045", "Ana Sofía López", "Ingeniería en Software", StudentStatus.APPROVED,
                consentVersion = "1.0", consentTimestampMs = TestTime.at(2026, 9, 1, 9),
                createdAtMs = TestTime.at(2026, 9, 1, 9), approvedAtMs = TestTime.at(2026, 9, 2, 8), approvedByGuard = "Caseta Norte"
            ),
            sampleCount = 5,
            qr = QrUi(QrRenderer.render("UTCJ1|20231045|1791480000|demo").asImageBitmap(), 18, 30),
            onShowQr = {}, onHideQr = {}, onBack = {}, onReRegister = {}, onDeleteData = {}, onSignOut = {}
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
            onFingerprint = {}, onQr = {}, onFace = {}, onCallGuard = {}, onDismissResult = {}, onExit = {},
            cameraContent = { CameraPlaceholder() }
        )
    }

    @Test
    @Config(qualifiers = KIOSK)
    fun s06_kioskGranted() = shot("06-kiosco-acceso-permitido") {
        KioskContent(
            state = KioskViewModel.Ui(
                result = KioskViewModel.VerifyOutcome(true, "Ana Sofía López", "20231045", timeMs = TestTime.at(2026, 10, 8, 7, 42)),
                faceStatus = FaceGuideStatus.Done
            ),
            nowMs = TestTime.at(2026, 10, 8, 7, 42),
            sync = SyncUiState(SyncStatusLabel.ONLINE, 0, "En línea"),
            secondsLeft = 7, totalSeconds = 10,
            onFingerprint = {}, onQr = {}, onFace = {}, onCallGuard = {}, onDismissResult = {}, onExit = {},
            cameraContent = { CameraPlaceholder() }
        )
    }

    @Test
    @Config(qualifiers = KIOSK)
    fun s07_kioskDenied() = shot("07-kiosco-acceso-denegado") {
        KioskContent(
            state = KioskViewModel.Ui(
                result = KioskViewModel.VerifyOutcome(
                    false, "Luis Ramírez Torres", "20220311", reason = "Estatus BAJA",
                    timeMs = TestTime.at(2026, 10, 8, 7, 44)
                ),
                faceStatus = FaceGuideStatus.Error
            ),
            nowMs = TestTime.at(2026, 10, 8, 7, 44),
            sync = SyncUiState(SyncStatusLabel.PENDING, 3, "Pendientes: 3"),
            secondsLeft = 9, totalSeconds = 10,
            onFingerprint = {}, onQr = {}, onFace = {}, onCallGuard = {}, onDismissResult = {}, onExit = {},
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
                    enrolled = FakeData.students.map { it.matricula }.toSet() - "20240077",
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
                ui = SettingsUi(liveness = true),
                sync = SyncUiState(SyncStatusLabel.ONLINE, 0, "En línea"),
                onChange = {}, onImportCsv = {}, onLoadSample = {}, onSyncNow = {},
                onChangePassword = {}, onLogout = {}, onEvalMode = {}, onKiosk = {}
            )
        }
    }

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
const val PHONE = "w393dp-h852dp-port-xxhdpi"
const val TABLET = "w1280dp-h800dp-land-xhdpi"
const val KIOSK = "w1280dp-h800dp-land-xhdpi"

/** Datos ficticios deterministas para las capturas. */
object FakeData {
    val NOW = TestTime.at(2026, 10, 8, 13, 20)

    val students = listOf(
        Student("20231045", "Ana Sofía López", "Ingeniería en Software", StudentStatus.APPROVED),
        Student("20230112", "Diego Hernández", "Mecatrónica", StudentStatus.APPROVED),
        Student("20240077", "María Fernanda Ruiz", "Negocios Internacionales", StudentStatus.PENDING),
        Student("20220311", "Luis Ramírez Torres", "Redes y Telecomunicaciones", StudentStatus.BAJA),
        Student("20230988", "Valeria Castillo", "Ingeniería en Software", StudentStatus.ACTIVO),
        Student("20241203", "Jorge Alberto Núñez", "Manufactura", StudentStatus.PENDING),
        Student("20210544", "Paola Gutiérrez", "Energías Renovables", StudentStatus.SUSPENDIDO),
        Student("20230671", "Ricardo Salinas", "Mantenimiento Industrial", StudentStatus.APPROVED),
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
                add(AccessEvent(id++, t, s.matricula, s.nombre, AccessResult.ALLOWED, AccessMethod.FACE, verifyDurationMs = 820))
                if (i % 9 == 0) add(AccessEvent(id++, t + 30_000, "—", "Desconocido", AccessResult.DENIED, AccessMethod.FACE))
            }
        }
        // Hoy: pico a las 7 y 13 h.
        val today = edu.utcj.acceso.util.TimeUtil.startOfDayMs(NOW)
        val hours = listOf(6 to 4, 7 to 21, 8 to 12, 9 to 6, 10 to 5, 11 to 4, 12 to 7, 13 to 9)
        hours.forEach { (h, n) ->
            repeat(n) { i ->
                val s = names[(h + i) % names.size]
                val t = today + h * 3_600_000L + i * 150_000L
                val method = when (i % 11) { 3 -> AccessMethod.QR; 7 -> AccessMethod.MANUAL; else -> AccessMethod.FACE }
                val result = when (method) { AccessMethod.QR -> AccessResult.QR; AccessMethod.MANUAL -> AccessResult.MANUAL; else -> AccessResult.ALLOWED }
                add(
                    AccessEvent(
                        id++, t, s.matricula, s.nombre, result, method,
                        authorizingGuard = if (method == AccessMethod.MANUAL) "Carlos Méndez" else null,
                        reason = if (method == AccessMethod.MANUAL) "Credencial física verificada" else null,
                        similarity = if (method == AccessMethod.FACE) 0.78f else null,
                        verifyDurationMs = if (method == AccessMethod.FACE) 640L + (i * 37) % 400 else null
                    )
                )
            }
        }
        add(AccessEvent(id++, today + 7 * 3_600_000L + 600_000, "20220311", "Luis Ramírez Torres", AccessResult.DENIED, AccessMethod.FACE, reason = "Estatus BAJA"))
        repeat(3) { add(AccessEvent(id++, today + 13 * 3_600_000L + 900_000 + it * 40_000L, "20210544", "Paola Gutiérrez", AccessResult.DENIED, AccessMethod.FACE, reason = "Estatus SUSPENDIDO")) }
        add(AccessEvent(id++, today + 12 * 3_600_000L + 300_000, "20240077", "María Fernanda Ruiz", AccessResult.DENIED, AccessMethod.FACE, reason = "Pendiente de aprobación"))
    }
}
