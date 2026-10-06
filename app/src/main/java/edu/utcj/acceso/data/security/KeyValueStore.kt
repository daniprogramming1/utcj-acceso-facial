package edu.utcj.acceso.data.security

/**
 * Abstracción mínima de almacenamiento clave-valor.
 * Producción: [SecurePrefs] (EncryptedSharedPreferences). Pruebas: mapa en memoria.
 */
interface KeyValueStore {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String)
    fun getLong(key: String, default: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getInt(key: String, default: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
}
