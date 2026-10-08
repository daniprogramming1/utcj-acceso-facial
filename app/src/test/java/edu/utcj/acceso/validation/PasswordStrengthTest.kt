package edu.utcj.acceso.validation

import edu.utcj.acceso.data.security.PasswordStrength
import edu.utcj.acceso.data.security.PasswordStrength.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordStrengthTest {
    @Test
    fun shortPasswordsAreVeryWeak() {
        assertEquals(Level.VERY_WEAK, PasswordStrength.evaluate(""))
        assertEquals(Level.VERY_WEAK, PasswordStrength.evaluate("Ab1!"))
    }

    @Test
    fun repeatedCharactersAreWeak() {
        assertEquals(Level.WEAK, PasswordStrength.evaluate("aaaaaaaaaaaa"))
    }

    @Test
    fun strengthGrowsWithLengthAndVariety() {
        val simple = PasswordStrength.evaluate("guardia")
        val better = PasswordStrength.evaluate("Guardia2026")
        val best = PasswordStrength.evaluate("Caseta#Norte-2026")
        assertTrue(simple.score < better.score)
        assertTrue(better.score <= best.score)
        assertEquals(Level.VERY_STRONG, best)
    }
}
