package edu.utcj.acceso.testutil

import edu.utcj.acceso.data.security.KeyValueStore

/** Almacén clave-valor en memoria para pruebas (sustituye a SecurePrefs/Keystore). */
class MemoryKeyValueStore : KeyValueStore {
    val map = mutableMapOf<String, Any>()
    override fun getString(key: String, default: String?) = map[key] as? String ?: default
    override fun putString(key: String, value: String) { map[key] = value }
    override fun getLong(key: String, default: Long) = map[key] as? Long ?: default
    override fun putLong(key: String, value: Long) { map[key] = value }
    override fun getInt(key: String, default: Int) = map[key] as? Int ?: default
    override fun putInt(key: String, value: Int) { map[key] = value }
    override fun getBoolean(key: String, default: Boolean) = map[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getFloat(key: String, default: Float) = map[key] as? Float ?: default
    override fun putFloat(key: String, value: Float) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
    override fun contains(key: String) = map.containsKey(key)
}
