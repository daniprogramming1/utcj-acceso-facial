package edu.utcj.acceso.security

import edu.utcj.acceso.data.security.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun hashAndVerify_success() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash("secreto123".toCharArray(), salt)
        assertTrue(PasswordHasher.verify("secreto123".toCharArray(), salt, hash))
    }

    @Test
    fun wrongPassword_fails() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash("secreto123".toCharArray(), salt)
        assertFalse(PasswordHasher.verify("otra".toCharArray(), salt, hash))
    }

    @Test
    fun differentSalts_differentHashes() {
        val h1 = PasswordHasher.hash("x".toCharArray(), PasswordHasher.generateSalt())
        val h2 = PasswordHasher.hash("x".toCharArray(), PasswordHasher.generateSalt())
        assertFalse(PasswordHasher.constantTimeEquals(h1, h2))
    }

    @Test
    fun hexRoundTrip() {
        val bytes = byteArrayOf(0x0a, 0x1b, 0xff.toByte())
        val hex = PasswordHasher.toHex(bytes)
        val back = PasswordHasher.fromHex(hex)
        assertTrue(PasswordHasher.constantTimeEquals(bytes, back))
    }
}
