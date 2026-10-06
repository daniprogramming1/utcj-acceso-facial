package edu.utcj.acceso.data.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guard password auth with PBKDF2 + lockout after 5 failed attempts (5 minutes).
 */
@Singleton
class GuardAuthManager @Inject constructor(
    private val securePrefs: KeyValueStore
) {
    /** Reloj inyectable para pruebas de bloqueo. */
    internal var clock: () -> Long = { System.currentTimeMillis() }

    companion object {
        private const val KEY_HASH = "guard_password_hash"
        private const val KEY_SALT = "guard_password_salt"
        private const val KEY_FAILS = "guard_fail_count"
        private const val KEY_LOCK_UNTIL = "guard_lock_until_ms"
        private const val KEY_SETUP_DONE = "guard_password_setup_done"
        private const val KEY_GUARD_NAME = "guard_display_name"
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MS = 5 * 60 * 1000L
        const val DEFAULT_GUARD_ID = "guardia_1"
    }

    fun isPasswordSet(): Boolean = securePrefs.getBoolean(KEY_SETUP_DONE, false)

    fun isLockedOut(): Boolean {
        val until = securePrefs.getLong(KEY_LOCK_UNTIL, 0L)
        return clock() < until
    }

    fun lockRemainingMs(): Long {
        val until = securePrefs.getLong(KEY_LOCK_UNTIL, 0L)
        return (until - clock()).coerceAtLeast(0L)
    }

    fun failedAttempts(): Int = securePrefs.getInt(KEY_FAILS, 0)

    fun setPassword(password: CharArray, displayName: String = "Guardia UTCJ"): AuthOutcome {
        if (password.size < 6) return AuthOutcome.Error("La contraseña debe tener al menos 6 caracteres")
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(password, salt)
        securePrefs.putString(KEY_SALT, PasswordHasher.toHex(salt))
        securePrefs.putString(KEY_HASH, PasswordHasher.toHex(hash))
        securePrefs.putBoolean(KEY_SETUP_DONE, true)
        securePrefs.putString(KEY_GUARD_NAME, displayName)
        securePrefs.putInt(KEY_FAILS, 0)
        securePrefs.putLong(KEY_LOCK_UNTIL, 0L)
        password.fill('\u0000')
        return AuthOutcome.Success(displayName)
    }

    fun changePassword(current: CharArray, newPassword: CharArray): AuthOutcome {
        val verify = verifyPassword(current)
        if (verify !is AuthOutcome.Success) return verify
        return setPassword(newPassword, getDisplayName())
    }

    fun verifyPassword(password: CharArray): AuthOutcome {
        if (isLockedOut()) {
            return AuthOutcome.Locked(lockRemainingMs())
        }
        if (!isPasswordSet()) return AuthOutcome.Error("Contraseña no configurada")
        val saltHex = securePrefs.getString(KEY_SALT) ?: return AuthOutcome.Error("Datos de seguridad corruptos")
        val hashHex = securePrefs.getString(KEY_HASH) ?: return AuthOutcome.Error("Datos de seguridad corruptos")
        val ok = PasswordHasher.verify(password, PasswordHasher.fromHex(saltHex), PasswordHasher.fromHex(hashHex))
        password.fill('\u0000')
        return if (ok) {
            securePrefs.putInt(KEY_FAILS, 0)
            AuthOutcome.Success(getDisplayName())
        } else {
            val fails = failedAttempts() + 1
            securePrefs.putInt(KEY_FAILS, fails)
            if (fails >= MAX_ATTEMPTS) {
                securePrefs.putLong(KEY_LOCK_UNTIL, clock() + LOCKOUT_MS)
                securePrefs.putInt(KEY_FAILS, 0)
                AuthOutcome.Locked(LOCKOUT_MS)
            } else {
                AuthOutcome.Error("Contraseña incorrecta. Intentos restantes: ${MAX_ATTEMPTS - fails}")
            }
        }
    }

    fun getDisplayName(): String = securePrefs.getString(KEY_GUARD_NAME, "Guardia UTCJ") ?: "Guardia UTCJ"
}

sealed class AuthOutcome {
    data class Success(val displayName: String) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
    data class Locked(val remainingMs: Long) : AuthOutcome()
}
