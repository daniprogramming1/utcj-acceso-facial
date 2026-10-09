package edu.utcj.acceso.domain.qr

import java.util.Base64

/**
 * Formato compacto de los códigos QR (sin servidor, verificable sin conexión en otro teléfono).
 *
 * Todos los campos van separados por «.»; los textos y bytes en Base64 URL sin relleno y las
 * fechas en segundos Unix. La firma (ECDSA P-256 / SHA-256, DER) cubre todo lo anterior al
 * último punto.
 *
 * - Registro: `UTCJR1.mat.nombre.carrera.correo.consentimiento.llavePublica.emitido.firma`
 *   (autofirmado con la llave privada del teléfono del alumno: prueba de posesión).
 * - Acceso:   `UTCJA1.mat.emitido.vence.nonce.firma`
 */
object QrCodec {
    const val REGISTRATION_PREFIX = "UTCJR1"
    const val ACCESS_PREFIX = "UTCJA1"
    private const val SEP = "."

    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    fun b64(bytes: ByteArray): String = enc.encodeToString(bytes)
    fun b64(text: String): String = b64(text.toByteArray(Charsets.UTF_8))
    private fun bytes(s: String): ByteArray = dec.decode(s)
    private fun text(s: String): String = String(bytes(s), Charsets.UTF_8)

    // ---------- Registro ----------

    fun registrationSignedPart(p: RegistrationPayload): String = listOf(
        REGISTRATION_PREFIX,
        b64(p.matricula),
        b64(p.nombre),
        b64(p.carrera),
        b64(p.correo.orEmpty()),
        b64(p.consentVersion),
        b64(p.publicKey),
        (p.issuedAtMs / 1000).toString()
    ).joinToString(SEP)

    // ---------- Acceso ----------

    fun accessSignedPart(t: AccessToken): String = listOf(
        ACCESS_PREFIX,
        b64(t.matricula),
        (t.issuedAtMs / 1000).toString(),
        (t.expiresAtMs / 1000).toString(),
        t.nonce
    ).joinToString(SEP)

    fun withSignature(signedPart: String, signature: ByteArray): String = signedPart + SEP + b64(signature)

    /** Interpreta cualquier QR leído. Nunca lanza excepciones. */
    fun parse(raw: String): QrParseResult {
        val text = raw.trim()
        val parts = text.split(SEP)
        return try {
            when (parts.firstOrNull()) {
                REGISTRATION_PREFIX -> {
                    if (parts.size != 9) return QrParseResult.Invalid
                    val payload = RegistrationPayload(
                        matricula = text(parts[1]),
                        nombre = text(parts[2]),
                        carrera = text(parts[3]),
                        correo = text(parts[4]).ifBlank { null },
                        consentVersion = text(parts[5]),
                        publicKey = bytes(parts[6]),
                        issuedAtMs = parts[7].toLong() * 1000
                    )
                    if (payload.matricula.isBlank() || payload.publicKey.isEmpty()) return QrParseResult.Invalid
                    QrParseResult.Registration(payload, text.substringBeforeLast(SEP), bytes(parts[8]))
                }
                ACCESS_PREFIX -> {
                    if (parts.size != 6) return QrParseResult.Invalid
                    val token = AccessToken(
                        matricula = text(parts[1]),
                        issuedAtMs = parts[2].toLong() * 1000,
                        expiresAtMs = parts[3].toLong() * 1000,
                        nonce = parts[4].also { require(it.length in 8..64) }
                    )
                    if (token.matricula.isBlank()) return QrParseResult.Invalid
                    QrParseResult.Access(token, text.substringBeforeLast(SEP), bytes(parts[5]))
                }
                else -> QrParseResult.Invalid
            }
        } catch (e: IllegalArgumentException) { // Base64 o números inválidos (NumberFormatException incluida)
            QrParseResult.Invalid
        }
    }
}

/** Datos que el alumno presenta al guardia para activar su acceso. */
data class RegistrationPayload(
    val matricula: String,
    val nombre: String,
    val carrera: String,
    val correo: String?,
    val consentVersion: String,
    /** Llave pública EC P-256 en formato X.509 (SubjectPublicKeyInfo). */
    val publicKey: ByteArray,
    val issuedAtMs: Long
) {
    override fun equals(other: Any?): Boolean = other is RegistrationPayload &&
        matricula == other.matricula && nombre == other.nombre && carrera == other.carrera &&
        correo == other.correo && consentVersion == other.consentVersion &&
        publicKey.contentEquals(other.publicKey) && issuedAtMs == other.issuedAtMs

    override fun hashCode(): Int = matricula.hashCode() * 31 + publicKey.contentHashCode()
}

/** Pase de acceso temporal firmado por el teléfono del alumno. */
data class AccessToken(
    val matricula: String,
    val issuedAtMs: Long,
    val expiresAtMs: Long,
    /** Valor aleatorio de un solo uso (protección contra repetición). */
    val nonce: String
) {
    val lifetimeMs: Long get() = expiresAtMs - issuedAtMs
}

sealed class QrParseResult {
    data class Registration(val payload: RegistrationPayload, val signedPart: String, val signature: ByteArray) : QrParseResult()
    data class Access(val token: AccessToken, val signedPart: String, val signature: ByteArray) : QrParseResult()
    data object Invalid : QrParseResult()
}
