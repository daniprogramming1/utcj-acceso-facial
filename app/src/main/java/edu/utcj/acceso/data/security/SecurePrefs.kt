package edu.utcj.acceso.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias cifradas (AES-256 GCM/SIV con clave en Android Keystore).
 *
 * Si el archivo cifrado no se puede abrir (p. ej. keyset dañado o la clave del Keystore ya no
 * existe tras restaurar/reinstalar → `AEADBadTagException`, `KeyStoreException`,
 * `InvalidProtocolBufferException`), se borran el archivo y la clave maestra y se crean de nuevo
 * **vacíos**. Nunca se recurre a preferencias sin cifrar: el guardia deberá configurar otra vez su
 * contraseña y la configuración vuelve a los valores predeterminados.
 */
@Singleton
class SecurePrefs @Inject constructor(
    @ApplicationContext context: Context
) : KeyValueStore {

    /** `true` si al arrancar hubo que recrear el almacén por estar dañado. */
    var wasReset: Boolean = false
        private set

    val prefs: SharedPreferences = openWithRecovery(context)

    private fun open(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also { it.all } // fuerza descifrado para detectar daño ahora y no en una lectura posterior
    }

    private fun openWithRecovery(context: Context): SharedPreferences = try {
        open(context)
    } catch (e: Exception) {
        if (!isRecoverable(e)) throw e
        Log.e(TAG, "Preferencias cifradas dañadas o ilegibles; se recrean vacías (se pedirá configurar de nuevo la contraseña del guardia).", e)
        wasReset = true
        context.deleteSharedPreferences(FILE_NAME)
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }.onFailure { Log.w(TAG, "No se pudo borrar la clave maestra", it) }
        open(context)
    }

    override fun getString(key: String, default: String?): String? = prefs.getString(key, default)
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    override fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    override fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
    override fun contains(key: String): Boolean = prefs.contains(key)

    companion object {
        private const val TAG = "SecurePrefs"
        const val FILE_NAME = "acceso_utcj_secure"

        /** Errores de cifrado/keyset que justifican recrear el almacén (incluye causas anidadas). */
        internal fun isRecoverable(e: Throwable): Boolean {
            var t: Throwable? = e
            while (t != null) {
                if (t is GeneralSecurityException || t is IOException || t is SecurityException) return true
                t = t.cause
            }
            return false
        }
    }
}
