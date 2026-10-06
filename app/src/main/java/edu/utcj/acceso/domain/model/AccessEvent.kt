package edu.utcj.acceso.domain.model

data class AccessEvent(
    val id: Long = 0,
    val datetimeMs: Long = System.currentTimeMillis(),
    val matricula: String,
    val nombre: String,
    val result: AccessResult,
    val method: AccessMethod,
    val authorizingGuard: String? = null,
    val reason: String? = null,
    val similarity: Float? = null,
    val verifyDurationMs: Long? = null,
    val synced: Boolean = false
)

enum class AccessResult {
    ALLOWED,
    DENIED,
    MANUAL,
    QR
}

enum class AccessMethod {
    FACE,
    FINGERPRINT,
    QR,
    MANUAL,
    FALLBACK
}
