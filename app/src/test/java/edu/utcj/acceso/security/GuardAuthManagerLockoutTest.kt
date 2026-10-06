package edu.utcj.acceso.security

import edu.utcj.acceso.data.security.AuthOutcome
import edu.utcj.acceso.data.security.GuardAuthManager
import edu.utcj.acceso.data.security.KeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GuardAuthManagerLockoutTest {

    private class MemoryStore : KeyValueStore {
        val map = mutableMapOf<String, Any>()
        override fun getString(key: String, default: String?) = map[key] as? String ?: default
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getLong(key: String, default: Long) = map[key] as? Long ?: default
        override fun putLong(key: String, value: Long) { map[key] = value }
        override fun getInt(key: String, default: Int) = map[key] as? Int ?: default
        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getBoolean(key: String, default: Boolean) = map[key] as? Boolean ?: default
        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    }

    private var now = 1_000_000L
    private lateinit var auth: GuardAuthManager

    @Before
    fun setUp() {
        auth = GuardAuthManager(MemoryStore())
        auth.clock = { now }
        auth.setPassword("correcta1".toCharArray())
    }

    @Test
    fun correctPassword_succeeds() {
        assertTrue(auth.verifyPassword("correcta1".toCharArray()) is AuthOutcome.Success)
    }

    @Test
    fun shortPassword_rejectedOnSetup() {
        assertTrue(auth.setPassword("123".toCharArray()) is AuthOutcome.Error)
    }

    @Test
    fun fiveFailures_locksForFiveMinutes() {
        repeat(4) {
            assertTrue(auth.verifyPassword("mala".toCharArray()) is AuthOutcome.Error)
        }
        val fifth = auth.verifyPassword("mala".toCharArray())
        assertTrue(fifth is AuthOutcome.Locked)
        assertTrue(auth.isLockedOut())

        // Durante el bloqueo, incluso la contraseña correcta es rechazada
        now += 4 * 60 * 1000L
        assertTrue(auth.verifyPassword("correcta1".toCharArray()) is AuthOutcome.Locked)

        // Pasados 5 minutos se desbloquea
        now += 61 * 1000L
        assertFalse(auth.isLockedOut())
        assertTrue(auth.verifyPassword("correcta1".toCharArray()) is AuthOutcome.Success)
    }

    @Test
    fun successResetsFailCounter() {
        repeat(3) { auth.verifyPassword("mala".toCharArray()) }
        auth.verifyPassword("correcta1".toCharArray())
        assertEquals(0, auth.failedAttempts())
    }

    @Test
    fun changePassword_requiresCurrent() {
        assertTrue(auth.changePassword("mala".toCharArray(), "nueva123".toCharArray()) is AuthOutcome.Error)
        assertTrue(auth.changePassword("correcta1".toCharArray(), "nueva123".toCharArray()) is AuthOutcome.Success)
        assertTrue(auth.verifyPassword("nueva123".toCharArray()) is AuthOutcome.Success)
    }

    @Test
    fun policyConstants() {
        assertEquals(5, GuardAuthManager.MAX_ATTEMPTS)
        assertEquals(5 * 60 * 1000L, GuardAuthManager.LOCKOUT_MS)
    }
}
