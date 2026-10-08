package edu.utcj.acceso.security

import edu.utcj.acceso.data.security.SecurePrefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.security.KeyStoreException
import javax.crypto.AEADBadTagException

/** Qué errores al abrir EncryptedSharedPreferences provocan recrear el almacén (y cuáles no). */
class SecurePrefsRecoveryTest {
    @Test
    fun keysetCorruption_isRecoverable() {
        assertTrue(SecurePrefs.isRecoverable(AEADBadTagException("tag mismatch")))
        assertTrue(SecurePrefs.isRecoverable(KeyStoreException("clave inexistente")))
        assertTrue(SecurePrefs.isRecoverable(IOException("protobuf inválido")))
        assertTrue(SecurePrefs.isRecoverable(RuntimeException(AEADBadTagException("anidada"))))
        assertTrue(SecurePrefs.isRecoverable(SecurityException("Could not decrypt value")))
    }

    @Test
    fun programmingErrors_areNotSwallowed() {
        assertFalse(SecurePrefs.isRecoverable(IllegalStateException("bug")))
        assertFalse(SecurePrefs.isRecoverable(NullPointerException()))
    }
}
