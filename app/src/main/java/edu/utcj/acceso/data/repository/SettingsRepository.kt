package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.biometric.FaceMatcher
import edu.utcj.acceso.data.security.SecurePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App settings persisted in EncryptedSharedPreferences.
 *
 * ## Face threshold
 * Key: [KEY_FACE_THRESHOLD], default [FaceMatcher.DEFAULT_THRESHOLD] = **0.72**
 *
 * ## Access hours
 * [KEY_HOURS_START] / [KEY_HOURS_END] — 24h local (America/Ciudad_Juarez)
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val securePrefs: SecurePrefs
) {
    companion object {
        const val KEY_FACE_THRESHOLD = "face_match_threshold"
        const val KEY_LIVENESS = "liveness_enabled"
        const val KEY_HOURS_START = "access_hours_start"
        const val KEY_HOURS_END = "access_hours_end"
        const val KEY_QR_SECRET = "qr_hmac_secret"
        const val KEY_KIOSK_IDLE_MS = "kiosk_idle_ms"
        const val KEY_MIN_SAMPLES = "min_face_samples"
        const val KEY_MAX_SAMPLES = "max_face_samples"
        const val DEFAULT_HOURS_START = 6
        const val DEFAULT_HOURS_END = 22
        const val DEFAULT_KIOSK_IDLE_MS = 30_000L
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
    fun setKioskIdleMs(ms: Long) = securePrefs.putLong(KEY_KIOSK_IDLE_MS, ms)

    fun getMinSamples(): Int = securePrefs.getInt(KEY_MIN_SAMPLES, 3)
    fun getMaxSamples(): Int = securePrefs.getInt(KEY_MAX_SAMPLES, 5)

    fun getOrCreateQrSecret(): String {
        val existing = securePrefs.getString(KEY_QR_SECRET)
        if (!existing.isNullOrBlank()) return existing
        val secret = java.util.UUID.randomUUID().toString().replace("-", "") +
            java.util.UUID.randomUUID().toString().replace("-", "")
        securePrefs.putString(KEY_QR_SECRET, secret)
        return secret
    }
}
