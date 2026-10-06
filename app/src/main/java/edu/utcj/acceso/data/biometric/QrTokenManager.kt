package edu.utcj.acceso.data.biometric

import android.util.Base64
import edu.utcj.acceso.data.repository.SettingsRepository
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic QR: HMAC-SHA256 signed token with 30s expiry.
 * Payload format: matricula|expiryEpochSec|nonce
 * Token: base64url(payload).base64url(hmac)
 */
@Singleton
class QrTokenManager @Inject constructor(
    private val settings: SettingsRepository
) {
    companion object {
        const val VALIDITY_SECONDS = 30L
    }

    fun issue(matricula: String, nowSec: Long = System.currentTimeMillis() / 1000): String {
        val expiry = nowSec + VALIDITY_SECONDS
        val nonce = java.util.UUID.randomUUID().toString().take(8)
        val payload = "$matricula|$expiry|$nonce"
        val sig = hmac(payload)
        return b64(payload.toByteArray(StandardCharsets.UTF_8)) + "." + b64(sig)
    }

    fun verify(token: String, nowSec: Long = System.currentTimeMillis() / 1000): QrVerifyResult {
        val parts = token.split('.')
        if (parts.size != 2) return QrVerifyResult.Invalid("Formato QR inválido")
        val payloadBytes = try {
            Base64.decode(parts[0], Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (e: Exception) {
            return QrVerifyResult.Invalid("QR corrupto")
        }
        val payload = String(payloadBytes, StandardCharsets.UTF_8)
        val expected = hmac(payload)
        val actual = try {
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (e: Exception) {
            return QrVerifyResult.Invalid("Firma QR inválida")
        }
        if (!edu.utcj.acceso.data.security.PasswordHasher.constantTimeEquals(expected, actual)) {
            return QrVerifyResult.Invalid("Firma QR no coincide")
        }
        val bits = payload.split('|')
        if (bits.size != 3) return QrVerifyResult.Invalid("Payload incompleto")
        val matricula = bits[0]
        val expiry = bits[1].toLongOrNull() ?: return QrVerifyResult.Invalid("Expiración inválida")
        if (nowSec > expiry) return QrVerifyResult.Invalid("QR expirado")
        return QrVerifyResult.Valid(matricula)
    }

    private fun hmac(payload: String): ByteArray {
        val secret = settings.getOrCreateQrSecret()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
    }

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

sealed class QrVerifyResult {
    data class Valid(val matricula: String) : QrVerifyResult()
    data class Invalid(val reason: String) : QrVerifyResult()
}
