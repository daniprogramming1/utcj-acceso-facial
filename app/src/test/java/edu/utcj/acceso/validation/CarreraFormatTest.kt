package edu.utcj.acceso.validation

import edu.utcj.acceso.domain.validation.RegistrationValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CarreraFormatTest {

    @Test
    fun grupoIsOptionalAndNormalized() {
        assertNull(RegistrationValidator.grupoError(""))
        assertNull(RegistrationValidator.grupoError("5a"))
        assertNull(RegistrationValidator.grupoError(" 10 bt "))
        assertNotNull(RegistrationValidator.grupoError("A5"))
        assertNotNull(RegistrationValidator.grupoError("5"))
        assertNotNull(RegistrationValidator.grupoError("123A"))
    }

    @Test
    fun joinAndSplitRoundTrip() {
        val stored = RegistrationValidator.joinCarrera("Ingeniería en Software ", "5a")
        assertEquals("Ingeniería en Software · 5A", stored)
        assertEquals("Ingeniería en Software" to "5A", RegistrationValidator.splitCarrera(stored))
    }

    @Test
    fun splitKeepsLegacyFreeTextAsCarrera() {
        // Registros de la versión anterior (texto libre) no deben perderse al precargar.
        assertEquals("TI" to "", RegistrationValidator.splitCarrera("TI"))
        assertEquals("Redes · Telecom" to "", RegistrationValidator.splitCarrera("Redes · Telecom"))
        assertEquals("Mecatrónica", RegistrationValidator.joinCarrera("Mecatrónica", ""))
        assertEquals("", RegistrationValidator.joinCarrera("", ""))
    }

    @Test
    fun tooShortMatriculaUsesTheSharedMessage() {
        assertEquals(RegistrationValidator.MATRICULA_TOO_SHORT, RegistrationValidator.matriculaError("202"))
    }
}
