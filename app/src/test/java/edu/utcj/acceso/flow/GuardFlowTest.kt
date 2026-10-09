package edu.utcj.acceso.flow

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.mlkit.common.MlKit
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import edu.utcj.acceso.MainActivity
import edu.utcj.acceso.data.local.AccessEventDao
import edu.utcj.acceso.data.local.AccessEventEntity
import edu.utcj.acceso.data.local.StudentDao
import edu.utcj.acceso.data.local.StudentEntity
import edu.utcj.acceso.data.qr.ExternalQrInput
import edu.utcj.acceso.data.repository.AuthRepository
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.security.KeyValueStore
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.QrCrypto
import edu.utcj.acceso.domain.qr.SoftwareQrSigner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject

/**
 * Recorrido real (Hilt + NavGraph + MainActivity) del personal de seguridad:
 * selección de rol → configuración inicial / inicio de sesión → panel → cada sección → kiosco.
 * Reproduce el cierre reportado al pulsar «Entrar al panel».
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, sdk = [34], qualifiers = "w393dp-h852dp-port-xxhdpi")
class GuardFlowTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var store: KeyValueStore
    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var events: AccessEventDao
    @Inject lateinit var students: StudentDao
    @Inject lateinit var qrInput: ExternalQrInput

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hilt.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        // En el teléfono ML Kit se inicializa con su ContentProvider; Robolectric no lo ejecuta.
        runCatching { MlKit.initialize(context) }
        store.putBoolean(SettingsRepository.KEY_ONBOARDING_DONE, true)
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun SemanticsNodeInteractionsProvider.waitForText(text: String, timeoutMs: Long = 15_000) {
        compose.waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun type(label: String, value: String) {
        compose.onNode(hasSetTextAction() and hasText(label)).performTextInput(value)
    }

    private fun openGuard() {
        compose.waitForText("Personal de seguridad")
        compose.onNodeWithText("Personal de seguridad").performClick()
    }


    /**
     * Fuerza pasadas de DIBUJO (Robolectric no dibuja por sí solo) recorriendo los fotogramas de las
     * animaciones del tablero, para que cualquier excepción de dibujo (NaN, tamaños negativos…) haga fallar la prueba.
     */
    private var drawn = 0

    /** Dibuja toda la ventana en un Bitmap (ejecuta los DrawScope de Compose). */
    private fun drawOnce() {
        compose.waitForIdle()
        scenario!!.onActivity { activity ->
            val root = activity.window.decorView
            val bmp = Bitmap.createBitmap(root.width.coerceAtLeast(1), root.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bmp))
            bmp.recycle()
            drawn++
        }
    }

    private fun drawFrames(frames: Int = 40, stepMs: Long = 40) {
        compose.mainClock.autoAdvance = false
        try {
            repeat(frames) {
                compose.mainClock.advanceTimeBy(stepMs)
                drawOnce()
            }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        check(drawn > 0) { "No se dibujó ningún fotograma" }
    }

    /** Espera a que el tablero termine de cargar y recorre todas las secciones del panel. */
    private fun assertPanelWorks(expectEmpty: Boolean) {
        compose.waitForText("Actividad reciente")
        drawFrames() // estado de carga (esqueletos) y animación de entrada de las gráficas
        if (expectEmpty) compose.waitForText("Sin actividad")
        compose.waitForText("Accesos por hora")
        compose.waitForIdle()
        drawFrames()

        for ((nav, marker) in listOf(
            "Escanear" to "Escanear QR",
            "Aprobar" to "Aprobaciones",
            "Bitácora" to "Bitácora",
            "Más" to "Más opciones"
        )) {
            compose.onNode(hasText(nav) and hasClickAction(), useUnmergedTree = false).performClick()
            compose.waitForText(marker)
            compose.waitForIdle()
            drawFrames(frames = 10)
        }
        for (item in listOf("Alumnos", "Visitantes", "Incidencias", "Configuración")) {
            compose.onNodeWithText("Más").performClick()
            compose.waitForText("Más opciones")
            compose.onNodeWithText(item).performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Inicio").performClick()
        compose.waitForText("Accesos por hora")
        drawFrames()
    }

    @Test
    fun firstRun_setupPassword_opensEmptyPanel() {
        launch()
        openGuard()
        compose.waitForText("Configura el acceso")
        type("Nueva contraseña", "Seguro#2026")
        type("Confirmar contraseña", "Seguro#2026")
        compose.onNodeWithText("Guardar y entrar").performClick()
        assertPanelWorks(expectEmpty = true)
    }

    @Test
    fun login_withExistingPassword_opensEmptyPanel() {
        auth.setupPassword("Seguro#2026".toCharArray(), "Guardia Norte")
        launch()
        openGuard()
        compose.waitForText("Acceso de seguridad")
        type("Contraseña", "Seguro#2026")
        compose.onNode(hasText("Entrar al panel") and hasClickAction()).performClick()
        assertPanelWorks(expectEmpty = true)
    }

    @Test
    fun login_withData_opensPanel_andKiosk() {
        auth.setupPassword("Seguro#2026".toCharArray(), "Guardia Norte")
        val now = System.currentTimeMillis()
        runBlocking {
            students.upsert(StudentEntity(matricula = "20231234", nombre = "Ana López", status = StudentStatus.PENDING))
            repeat(4) { i ->
                events.insert(
                    AccessEventEntity(
                        datetimeMs = now - i * 60_000L, matricula = "20230001", nombre = "Luis Pérez",
                        result = if (i == 0) AccessResult.ALLOWED else AccessResult.DENIED,
                        method = AccessMethod.QR, verifyDurationMs = 120, syncKey = "k$i"
                    )
                )
            }
        }
        launch()
        openGuard()
        compose.waitForText("Acceso de seguridad")
        type("Contraseña", "Seguro#2026")
        compose.onNode(hasText("Entrar al panel") and hasClickAction()).performClick()
        assertPanelWorks(expectEmpty = false)
        compose.onNodeWithText("Modo kiosco").performClick()
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun tablet_login_opensEmptyPanel_withDrawer() {
        auth.setupPassword("Seguro#2026".toCharArray(), "Guardia Norte")
        launch()
        openGuard()
        compose.waitForText("Acceso de seguridad")
        type("Contraseña", "Seguro#2026")
        compose.onNode(hasText("Entrar al panel") and hasClickAction()).performClick()
        compose.waitForText("Sin actividad")
        drawFrames()
        for (item in listOf("Escanear QR", "Alumnos", "Aprobaciones", "Bitácora", "Visitantes", "Incidencias", "Configuración", "Inicio")) {
            compose.onNode(hasText(item) and hasClickAction()).performClick()
            compose.waitForIdle()
            drawFrames(frames = 10)
        }
        compose.waitForText("Accesos por hora")
    }

    private fun login() {
        auth.setupPassword("Seguro#2026".toCharArray(), "Guardia Norte")
        launch()
        openGuard()
        compose.waitForText("Acceso de seguridad")
        type("Contraseña", "Seguro#2026")
        compose.onNode(hasText("Entrar al panel") and hasClickAction()).performClick()
        compose.waitForText("Actividad reciente")
    }

    /**
     * Flujo completo con dos «teléfonos»: el alumno genera su QR de registro (llave EC en
     * software), el guardia lo escanea desde Aprobaciones y lo aprueba; luego escanea un QR de
     * acceso firmado y ve la tarjeta del alumno con «Acceso permitido».
     */
    @Test
    fun guard_approvesRegistrationQr_thenScansAccessQr_showsStudentCard() {
        val phone = SoftwareQrSigner.generate()
        val now = System.currentTimeMillis()
        // Horario que incluye la hora actual, para que la prueba pase a cualquier hora.
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        SettingsRepository(store).setHours(hour, (hour + 2) % 24)
        val registration = QrCrypto.registrationQr(phone, "20249999", "Ana López Núñez", "TI · 5A", "ana@utcj.edu.mx", "2.0.0", now)
        login()

        compose.onNode(hasText("Aprobar") and hasClickAction()).performClick()
        compose.waitForText("Escanear QR")
        compose.onNodeWithText("Escanea el QR de registro del alumno para ver sus datos y aprobarlo.").performClick()
        compose.waitForText("Acceso y registro de alumnos")
        drawFrames(frames = 5)

        qrInput.submit(registration)
        compose.waitForText("QR de registro")
        compose.waitForText("Ana López Núñez")
        clickButton("Aprobar acceso")
        compose.waitForText("Ana López Núñez ya puede entrar con su QR")
        val saved = runBlocking { students.getByMatricula("20249999") }
        assertNotNull(saved)
        assertEquals(StudentStatus.APPROVED, saved!!.status)
        assertNotNull(saved.publicKey)

        qrInput.submit(QrCrypto.accessQr(phone, "20249999", 60, System.currentTimeMillis()))
        compose.waitForText("Acceso permitido")
        compose.waitForText("Matrícula 20249999")
        drawFrames(frames = 5)
        clickButton("Registrar entrada")
        compose.waitForText("Entrada registrada: Ana López Núñez")
        val logged = runBlocking { events.getSince(0) }
        check(logged.any { it.matricula == "20249999" && it.result == AccessResult.ALLOWED && it.method == AccessMethod.QR }) {
            "La entrada no quedó en la bitácora: $logged"
        }

        // Un QR vencido del mismo alumno se rechaza con el motivo.
        qrInput.submit(QrCrypto.accessQr(phone, "20249999", 30, System.currentTimeMillis() - 5 * 60_000))
        compose.waitForText("QR vencido")
        compose.waitForText("Acceso denegado")
    }

    private fun clickButton(text: String) {
        compose.waitForText(text)
        compose.onNode(hasText(text) and hasClickAction()).performClick()
    }
}
