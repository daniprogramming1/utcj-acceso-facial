package edu.utcj.acceso.data.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption for face embeddings using Android Keystore.
 * Original photos are never persisted; only ciphertext + IV go to Room.
 */
@Singleton
class EmbeddingCrypto @Inject constructor() {
    companion object {
        private const val KEY_ALIAS = "utcj_face_embedding_aes"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val IV_BYTES = 12
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGen.init(spec)
        return keyGen.generateKey()
    }

    data class EncryptedBlob(val ciphertext: ByteArray, val iv: ByteArray)

    fun encrypt(plain: ByteArray): EncryptedBlob {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return EncryptedBlob(ct, iv)
    }

    fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun floatArrayToBytes(arr: FloatArray): ByteArray {
        val bytes = ByteArray(arr.size * 4)
        for (i in arr.indices) {
            val bits = java.lang.Float.floatToIntBits(arr[i])
            val o = i * 4
            bytes[o] = (bits shr 24).toByte()
            bytes[o + 1] = (bits shr 16).toByte()
            bytes[o + 2] = (bits shr 8).toByte()
            bytes[o + 3] = bits.toByte()
        }
        return bytes
    }

    fun bytesToFloatArray(bytes: ByteArray): FloatArray {
        require(bytes.size % 4 == 0)
        val arr = FloatArray(bytes.size / 4)
        for (i in arr.indices) {
            val o = i * 4
            val bits = ((bytes[o].toInt() and 0xFF) shl 24) or
                ((bytes[o + 1].toInt() and 0xFF) shl 16) or
                ((bytes[o + 2].toInt() and 0xFF) shl 8) or
                (bytes[o + 3].toInt() and 0xFF)
            arr[i] = java.lang.Float.intBitsToFloat(bits)
        }
        return arr
    }
}
