package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.security.KeyValueStore
import edu.utcj.acceso.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App settings persisted in EncryptedSharedPreferences.
 *
 * ## Vigencia del QR de acceso
 * - Alumno: [KEY_STUDENT_QR_VALIDITY] (por defecto [DEFAULT_STUDENT_QR_VALIDITY_SEC] = 60 s;
 *   opciones [STUDENT_QR_VALIDITY_OPTIONS]). Se cambia en «Mi acceso» → «Vigencia del QR».
 * - Guardia: [KEY_GUARD_MAX_QR_VALIDITY] (por defecto [DEFAULT_GUARD_MAX_QR_VALIDITY_SEC] = 5 min).
 *   Un QR que dure más que esto se rechaza. Se cambia en Configuración → «Acceso con QR».
 *
 * ## Access hours
 * [KEY_HOURS_START] / [KEY_HOURS_END] — 24h local (America/Ciudad_Juarez)
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val securePrefs: KeyValueStore
) {
    companion object {
        const val KEY_HOURS_START = "access_hours_start"
        const val KEY_HOURS_END = "access_hours_end"
        const val KEY_STUDENT_QR_VALIDITY = "student_qr_validity_sec"
        const val KEY_GUARD_MAX_QR_VALIDITY = "guard_max_qr_validity_sec"
        const val KEY_STUDENT_APPROVED_HINT = "student_marked_approved"
        const val KEY_KIOSK_IDLE_MS = "kiosk_idle_ms"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_THEME_MODE = "ui_theme_mode"
        const val KEY_KIOSK_ORIENTATION = "kiosk_orientation"
        const val KEY_KIOSK_SOUND = "kiosk_sound"
        const val KEY_STUDENT_MATRICULA = "student_remembered_matricula"
        const val DEFAULT_HOURS_START = 6
        const val DEFAULT_HOURS_END = 22
        /** Tiempo que el resultado (verde/rojo) permanece en el kiosco antes de volver a la cámara. */
        const val DEFAULT_KIOSK_IDLE_MS = 10_000L
        val KIOSK_IDLE_OPTIONS_MS = listOf(5_000L, 10_000L, 15_000L, 30_000L)
        const val DEFAULT_STUDENT_QR_VALIDITY_SEC = 60
        val STUDENT_QR_VALIDITY_OPTIONS = listOf(30, 60, 120, 300)
        const val DEFAULT_GUARD_MAX_QR_VALIDITY_SEC = 300
        val GUARD_MAX_QR_VALIDITY_OPTIONS = listOf(30, 60, 120, 300)

        /** Claves de la versión facial (1.1.x) que se borran al actualizar. */
        val OBSOLETE_KEYS = listOf(
            "face_match_threshold", "use_legacy_face_embedding", "liveness_enabled",
            "qr_hmac_secret", "min_face_samples", "max_face_samples"
        )

        /** «30 s», «1 min», «2 min»… */
        fun validityLabel(sec: Int): String = if (sec < 60) "$sec s" else "${sec / 60} min"
    }

    /** Orientación del kiosco. Horizontal por defecto (tabletas en pedestal). */
    enum class KioskOrientation(val labelEs: String) {
        LANDSCAPE("Horizontal"),
        PORTRAIT("Vertical"),
        AUTO("Automática")
    }

    fun getHoursStart(): Int = securePrefs.getInt(KEY_HOURS_START, DEFAULT_HOURS_START)
    fun getHoursEnd(): Int = securePrefs.getInt(KEY_HOURS_END, DEFAULT_HOURS_END)
    fun setHours(start: Int, end: Int) {
        securePrefs.putInt(KEY_HOURS_START, start.coerceIn(0, 23))
        securePrefs.putInt(KEY_HOURS_END, end.coerceIn(0, 23))
    }

    fun getKioskIdleMs(): Long = securePrefs.getLong(KEY_KIOSK_IDLE_MS, DEFAULT_KIOSK_IDLE_MS)
    fun setKioskIdleMs(ms: Long) = securePrefs.putLong(KEY_KIOSK_IDLE_MS, ms.coerceIn(3_000L, 60_000L))

    fun getKioskOrientation(): KioskOrientation =
        runCatching { KioskOrientation.valueOf(securePrefs.getString(KEY_KIOSK_ORIENTATION) ?: "") }
            .getOrDefault(KioskOrientation.LANDSCAPE)

    fun setKioskOrientation(o: KioskOrientation) = securePrefs.putString(KEY_KIOSK_ORIENTATION, o.name)

    fun isKioskSoundEnabled(): Boolean = securePrefs.getBoolean(KEY_KIOSK_SOUND, true)
    fun setKioskSoundEnabled(enabled: Boolean) = securePrefs.putBoolean(KEY_KIOSK_SOUND, enabled)

    // ---- Experiencia de usuario ----

    fun isOnboardingDone(): Boolean = securePrefs.getBoolean(KEY_ONBOARDING_DONE, false)
    fun setOnboardingDone() = securePrefs.putBoolean(KEY_ONBOARDING_DONE, true)

    private val _themeMode = MutableStateFlow(readThemeMode())
    /** Tema elegido por el usuario: SYSTEM / LIGHT / DARK. */
    val themeModeFlow: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun readThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(securePrefs.getString(KEY_THEME_MODE) ?: "") }.getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        securePrefs.putString(KEY_THEME_MODE, mode.name)
        _themeMode.value = mode
    }

    /**
     * Matrícula recordada en este dispositivo para el «Inicio del alumno».
     * Solo la matrícula (sin datos biométricos); se borra con «Salir» o «Eliminar mis datos».
     */
    fun getRememberedStudent(): String? = securePrefs.getString(KEY_STUDENT_MATRICULA)?.takeIf { it.isNotBlank() }
    fun setRememberedStudent(matricula: String?) {
        if (matricula.isNullOrBlank()) securePrefs.remove(KEY_STUDENT_MATRICULA)
        else securePrefs.putString(KEY_STUDENT_MATRICULA, matricula)
    }

    // ---- QR de acceso ----

    fun getStudentQrValiditySec(): Int =
        securePrefs.getInt(KEY_STUDENT_QR_VALIDITY, DEFAULT_STUDENT_QR_VALIDITY_SEC)
            .takeIf { it in STUDENT_QR_VALIDITY_OPTIONS } ?: DEFAULT_STUDENT_QR_VALIDITY_SEC

    fun setStudentQrValiditySec(sec: Int) {
        if (sec in STUDENT_QR_VALIDITY_OPTIONS) securePrefs.putInt(KEY_STUDENT_QR_VALIDITY, sec)
    }

    fun getGuardMaxQrValiditySec(): Int =
        securePrefs.getInt(KEY_GUARD_MAX_QR_VALIDITY, DEFAULT_GUARD_MAX_QR_VALIDITY_SEC)
            .takeIf { it in GUARD_MAX_QR_VALIDITY_OPTIONS } ?: DEFAULT_GUARD_MAX_QR_VALIDITY_SEC

    fun setGuardMaxQrValiditySec(sec: Int) {
        if (sec in GUARD_MAX_QR_VALIDITY_OPTIONS) securePrefs.putInt(KEY_GUARD_MAX_QR_VALIDITY, sec)
    }

    /**
     * El alumno indicó «Ya me aprobaron» en su teléfono (el guardia aprueba en otro equipo,
     * así que este teléfono no puede saberlo). Solo cambia qué QR se muestra primero.
     */
    fun isStudentMarkedApproved(): Boolean = securePrefs.getBoolean(KEY_STUDENT_APPROVED_HINT, false)
    fun setStudentMarkedApproved(v: Boolean) = securePrefs.putBoolean(KEY_STUDENT_APPROVED_HINT, v)

    /** Borra preferencias de la versión facial (umbral, motor, secreto HMAC…). Idempotente. */
    fun purgeObsoleteKeys() {
        OBSOLETE_KEYS.forEach { if (securePrefs.contains(it)) securePrefs.remove(it) }
    }
}
