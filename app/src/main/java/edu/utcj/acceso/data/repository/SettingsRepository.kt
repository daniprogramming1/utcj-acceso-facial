package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.biometric.FaceMatcher
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
 * ## Face threshold
 * Key: [KEY_FACE_THRESHOLD], default [FaceMatcher.DEFAULT_THRESHOLD] = **0.60** (MobileFaceNet)
 *
 * ## Motor facial
 * [KEY_USE_LEGACY_EMBEDDING]: si true, fuerza el histograma legado aunque exista el TFLite.
 *
 * ## Access hours
 * [KEY_HOURS_START] / [KEY_HOURS_END] — 24h local (America/Ciudad_Juarez)
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val securePrefs: KeyValueStore
) {
    companion object {
        const val KEY_FACE_THRESHOLD = "face_match_threshold"
        const val KEY_USE_LEGACY_EMBEDDING = "use_legacy_face_embedding"
        const val KEY_LIVENESS = "liveness_enabled"
        const val KEY_HOURS_START = "access_hours_start"
        const val KEY_HOURS_END = "access_hours_end"
        const val KEY_QR_SECRET = "qr_hmac_secret"
        const val KEY_KIOSK_IDLE_MS = "kiosk_idle_ms"
        const val KEY_MIN_SAMPLES = "min_face_samples"
        const val KEY_MAX_SAMPLES = "max_face_samples"
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
    }

    /** Orientación del kiosco. Horizontal por defecto (tabletas en pedestal). */
    enum class KioskOrientation(val labelEs: String) {
        LANDSCAPE("Horizontal"),
        PORTRAIT("Vertical"),
        AUTO("Automática")
    }

    private val _threshold = MutableStateFlow(getFaceThreshold())
    val faceThresholdFlow: StateFlow<Float> = _threshold.asStateFlow()

    fun getFaceThreshold(): Float =
        securePrefs.getFloat(KEY_FACE_THRESHOLD, FaceMatcher.DEFAULT_THRESHOLD)

    fun setFaceThreshold(value: Float) {
        val v = value.coerceIn(0.5f, 0.95f)
        securePrefs.putFloat(KEY_FACE_THRESHOLD, v)
        _threshold.value = v
    }

    fun isLivenessEnabled(): Boolean = securePrefs.getBoolean(KEY_LIVENESS, false)
    fun setLivenessEnabled(enabled: Boolean) = securePrefs.putBoolean(KEY_LIVENESS, enabled)

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

    fun getMinSamples(): Int = securePrefs.getInt(KEY_MIN_SAMPLES, 3)
    fun getMaxSamples(): Int = securePrefs.getInt(KEY_MAX_SAMPLES, 5)

    /** Fuerza el motor de histograma legado (268-d) en lugar de MobileFaceNet. */
    fun useLegacyFaceEmbedding(): Boolean =
        securePrefs.getBoolean(KEY_USE_LEGACY_EMBEDDING, false)

    fun setUseLegacyFaceEmbedding(enabled: Boolean) =
        securePrefs.putBoolean(KEY_USE_LEGACY_EMBEDDING, enabled)

    fun getOrCreateQrSecret(): String {
        val existing = securePrefs.getString(KEY_QR_SECRET)
        if (!existing.isNullOrBlank()) return existing
        val secret = java.util.UUID.randomUUID().toString().replace("-", "") +
            java.util.UUID.randomUUID().toString().replace("-", "")
        securePrefs.putString(KEY_QR_SECRET, secret)
        return secret
    }
}
