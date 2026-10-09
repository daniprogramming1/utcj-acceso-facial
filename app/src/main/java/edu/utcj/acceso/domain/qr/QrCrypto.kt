package edu.utcj.acceso.domain.qr

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/**
 * Firma de los QR del alumno. En el teléfono la implementación usa Android Keystore
 * (llave privada no exportable); en pruebas JVM se usa [SoftwareQrSigner].
 */
interface QrSigner {
    /** Llave pública X.509 (SubjectPublicKeyInfo). */
    val publicKey: ByteArray
    fun sign(data: ByteArray): ByteArray
}

/** Almacén de llaves por matrícula (una llave por alumno registrado en el teléfono). */
interface QrKeyStore {
    fun getOrCreate(matricula: String): QrSigner
    fun get(matricula: String): QrSigner?
    fun delete(matricula: String)
}

object QrCrypto {
    const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private val random = SecureRandom()

    fun verify(publicKeyX509: ByteArray, data: ByteArray, signature: ByteArray): Boolean = try {
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyX509))
        Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(key)
            update(data)
            verify(signature)
        }
    } catch (e: Exception) {
        false // llave o firma mal formadas ⇒ firma inválida
    }

    /** Nonce aleatorio de 96 bits en Base64 URL. */
    fun newNonce(): String = ByteArray(12).also { random.nextBytes(it) }.let(QrCodec::b64)

    fun registrationQr(
        signer: QrSigner,
        matricula: String,
        nombre: String,
        carrera: String,
        correo: String?,
        consentVersion: String,
        nowMs: Long
    ): String {
        val payload = RegistrationPayload(matricula, nombre, carrera, correo, consentVersion, signer.publicKey, nowMs)
        val signed = QrCodec.registrationSignedPart(payload)
        return QrCodec.withSignature(signed, signer.sign(signed.toByteArray(Charsets.UTF_8)))
    }

    fun accessQr(signer: QrSigner, matricula: String, validitySeconds: Int, nowMs: Long, nonce: String = newNonce()): String {
        val iat = nowMs / 1000 * 1000
        val token = AccessToken(matricula, iat, iat + validitySeconds * 1000L, nonce)
        val signed = QrCodec.accessSignedPart(token)
        return QrCodec.withSignature(signed, signer.sign(signed.toByteArray(Charsets.UTF_8)))
    }

    /** El QR de registro está firmado con la llave que contiene (prueba de posesión). */
    fun verifyRegistration(r: QrParseResult.Registration): Boolean =
        verify(r.payload.publicKey, r.signedPart.toByteArray(Charsets.UTF_8), r.signature)
}

/** Firmador con llave en memoria (pruebas y vista previa; no se usa en producción). */
class SoftwareQrSigner(private val keyPair: KeyPair) : QrSigner {
    override val publicKey: ByteArray get() = keyPair.public.encoded
    override fun sign(data: ByteArray): ByteArray = Signature.getInstance(QrCrypto.SIGNATURE_ALGORITHM).run {
        initSign(keyPair.private)
        update(data)
        sign()
    }

    companion object {
        fun generate(): SoftwareQrSigner = SoftwareQrSigner(
            KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        )
    }
}

/** Llaves en memoria por matrícula (pruebas). */
class InMemoryQrKeyStore : QrKeyStore {
    private val keys = mutableMapOf<String, SoftwareQrSigner>()
    override fun getOrCreate(matricula: String): QrSigner = keys.getOrPut(matricula) { SoftwareQrSigner.generate() }
    override fun get(matricula: String): QrSigner? = keys[matricula]
    override fun delete(matricula: String) { keys.remove(matricula) }
}
