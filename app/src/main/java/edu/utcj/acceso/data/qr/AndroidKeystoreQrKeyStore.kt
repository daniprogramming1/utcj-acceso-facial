package edu.utcj.acceso.data.qr

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import edu.utcj.acceso.domain.qr.QrCrypto
import edu.utcj.acceso.domain.qr.QrKeyStore
import edu.utcj.acceso.domain.qr.QrSigner
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Llaves EC P-256 en Android Keystore, una por matrícula registrada en este teléfono.
 * La llave privada es NO exportable: solo puede firmar dentro del Keystore (TEE cuando existe).
 * Si el alumno cambia de teléfono o borra sus datos, debe registrarse de nuevo y mostrar su
 * nuevo QR de registro en caseta.
 */
@Singleton
class AndroidKeystoreQrKeyStore @Inject constructor() : QrKeyStore {
    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val PREFIX = "utcj_qr_"
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
    private fun alias(matricula: String) = PREFIX + matricula

    override fun get(matricula: String): QrSigner? {
        val ks = keyStore()
        val alias = alias(matricula)
        val priv = ks.getKey(alias, null) as? PrivateKey ?: return null
        val pub = ks.getCertificate(alias)?.publicKey?.encoded ?: return null
        return KeystoreSigner(priv, pub)
    }

    override fun getOrCreate(matricula: String): QrSigner = get(matricula) ?: run {
        val spec = KeyGenParameterSpec.Builder(alias(matricula), KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER).apply { initialize(spec) }.generateKeyPair()
        requireNotNull(get(matricula)) { "No se pudo crear la llave del QR" }
    }

    override fun delete(matricula: String) {
        runCatching { keyStore().deleteEntry(alias(matricula)) }
    }

    private class KeystoreSigner(private val key: PrivateKey, override val publicKey: ByteArray) : QrSigner {
        override fun sign(data: ByteArray): ByteArray = Signature.getInstance(QrCrypto.SIGNATURE_ALGORITHM).run {
            initSign(key)
            update(data)
            sign()
        }
    }
}
