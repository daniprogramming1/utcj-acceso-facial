package edu.utcj.acceso.qr

import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.QrCodec
import edu.utcj.acceso.domain.qr.QrCrypto
import edu.utcj.acceso.domain.qr.QrParseResult
import edu.utcj.acceso.domain.qr.RegistrationCheck
import edu.utcj.acceso.domain.qr.RegistrationDecider
import edu.utcj.acceso.domain.qr.SoftwareQrSigner
import edu.utcj.acceso.domain.qr.StudentRecord
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationQrTest {
    private val signer = SoftwareQrSigner.generate()
    private val now = 1_790_000_000_000L
    private fun qr(correo: String? = "ana@utcj.edu.mx") =
        QrCrypto.registrationQr(signer, "2024001", "Ana López Núñez", "TI · 5A", correo, "v2", now)

    @Test fun parse_roundTrip_withAccentsAndSeparators() {
        val r = QrCodec.parse(qr()) as QrParseResult.Registration
        assertEquals("2024001", r.payload.matricula)
        assertEquals("Ana López Núñez", r.payload.nombre)
        assertEquals("TI · 5A", r.payload.carrera)
        assertEquals("ana@utcj.edu.mx", r.payload.correo)
        assertEquals("v2", r.payload.consentVersion)
        assertEquals(now, r.payload.issuedAtMs)
        assertArrayEquals(signer.publicKey, r.payload.publicKey)
        assertTrue(QrCrypto.verifyRegistration(r))
    }

    @Test fun optionalEmail_isNull() {
        assertNull((QrCodec.parse(qr(null)) as QrParseResult.Registration).payload.correo)
    }

    @Test fun payload_isCompactEnoughForAQr() {
        assertTrue(qr().length < 400)
    }

    @Test fun tamperedName_failsSelfSignature() {
        val bad = qr().replace(QrCodec.b64("Ana López Núñez"), QrCodec.b64("Otro Alumno"))
        val r = QrCodec.parse(bad) as QrParseResult.Registration
        assertFalse(QrCrypto.verifyRegistration(r))
        assertTrue(RegistrationDecider.check(r, null) is RegistrationCheck.Invalid)
    }

    @Test fun swappedKey_failsSelfSignature() {
        val other = SoftwareQrSigner.generate()
        val bad = qr().replace(QrCodec.b64(signer.publicKey), QrCodec.b64(other.publicKey))
        assertTrue(RegistrationDecider.check(QrCodec.parse(bad) as QrParseResult.Registration, null) is RegistrationCheck.Invalid)
    }

    @Test fun csvBajaOrSuspendido_blocksApproval() {
        val r = QrCodec.parse(qr()) as QrParseResult.Registration
        listOf(StudentStatus.BAJA, StudentStatus.SUSPENDIDO).forEach { st ->
            val c = RegistrationDecider.check(r, StudentRecord("2024001", "Ana", "", st, null))
            assertEquals(st, (c as RegistrationCheck.Blocked).status)
        }
    }

    @Test fun newPhone_isFlaggedAsKeyReplacement() {
        val r = QrCodec.parse(qr()) as QrParseResult.Registration
        val old = StudentRecord("2024001", "Ana", "", StudentStatus.APPROVED, SoftwareQrSigner.generate().publicKey)
        assertTrue((RegistrationDecider.check(r, old) as RegistrationCheck.Ok).replacesKey)
        val same = old.copy(publicKey = signer.publicKey)
        assertFalse((RegistrationDecider.check(r, same) as RegistrationCheck.Ok).replacesKey)
        val csvOnly = old.copy(status = StudentStatus.ACTIVO, publicKey = null)
        assertFalse((RegistrationDecider.check(r, csvOnly) as RegistrationCheck.Ok).replacesKey)
    }

    @Test fun accessQr_isNotARegistration() {
        assertTrue(QrCodec.parse(QrCrypto.accessQr(signer, "2024001", 60, now)) is QrParseResult.Access)
    }
}
