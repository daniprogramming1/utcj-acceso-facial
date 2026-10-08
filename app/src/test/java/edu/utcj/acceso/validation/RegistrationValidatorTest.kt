package edu.utcj.acceso.validation

import edu.utcj.acceso.domain.validation.RegistrationValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationValidatorTest {
    @Test
    fun normalizesMatricula() {
        assertEquals("AB12345", RegistrationValidator.normalizeMatricula("  ab 123 45 "))
    }

    @Test
    fun matriculaRules() {
        assertNotNull(RegistrationValidator.matriculaError(""))
        assertNotNull(RegistrationValidator.matriculaError("AB-123"))
        assertNotNull(RegistrationValidator.matriculaError("A1"))
        assertNotNull(RegistrationValidator.matriculaError("A".repeat(16)))
        assertNull(RegistrationValidator.matriculaError("utcj2024"))
    }

    @Test
    fun nombreRules() {
        assertNotNull(RegistrationValidator.nombreError(" "))
        assertNotNull(RegistrationValidator.nombreError("Al"))
        assertNotNull(RegistrationValidator.nombreError("Ana 2"))
        assertNull(RegistrationValidator.nombreError("María José Ñúñez"))
    }

    @Test
    fun isValidCombinesBoth() {
        assertTrue(RegistrationValidator.isValid("A1234", "Ana López"))
        assertFalse(RegistrationValidator.isValid("A1234", ""))
    }
}
