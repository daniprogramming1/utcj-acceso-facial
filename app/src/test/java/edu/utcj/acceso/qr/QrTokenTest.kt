package edu.utcj.acceso.qr

import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.AccessDecider
import edu.utcj.acceso.domain.qr.AccessDecision
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.domain.qr.GuardPolicy
import edu.utcj.acceso.domain.qr.QrCodec
import edu.utcj.acceso.domain.qr.QrCrypto
import edu.utcj.acceso.domain.qr.QrParseResult
import edu.utcj.acceso.domain.qr.SoftwareQrSigner
import edu.utcj.acceso.domain.qr.StudentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Firma / verificación del QR de acceso y todas las razones de rechazo. */
class QrTokenTest {
    private val signer = SoftwareQrSigner.generate()
    private val now = 1_790_000_000_000L
    private val policy = GuardPolicy(maxLifetimeSec = 300)
    private fun student(status: StudentStatus = StudentStatus.APPROVED, key: ByteArray? = signer.publicKey) =
        StudentRecord("2024001", "Ana López", "TI", status, key)

    private fun access(qr: String) = QrCodec.parse(qr) as QrParseResult.Access

    private fun decide(
        qr: String,
        student: StudentRecord? = student(),
        at: Long = now,
        used: Boolean = false,
        hours: Boolean = true,
        p: GuardPolicy = policy
    ) = AccessDecider.decide(access(qr), student, p, at, used, hours)

    private fun reason(d: AccessDecision) = (d as AccessDecision.Denied).reason

    @Test fun validToken_isAllowed() {
        val d = decide(QrCrypto.accessQr(signer, "2024001", 60, now))
        assertTrue(d is AccessDecision.Allowed)
        assertTrue(d.consumesNonce)
    }

    @Test fun roundTrip_keepsFields() {
        val t = access(QrCrypto.accessQr(signer, "2024001", 60, now + 999, nonce = "abcdefghABCD")).token
        assertEquals("2024001", t.matricula)
        assertEquals(now, t.issuedAtMs)
        assertEquals(now + 60_000, t.expiresAtMs)
        assertEquals("abcdefghABCD", t.nonce)
    }

    @Test fun expiredToken_isDenied_afterSkew() {
        val qr = QrCrypto.accessQr(signer, "2024001", 60, now)
        // Dentro de la tolerancia de 30 s aún es válido…
        assertTrue(decide(qr, at = now + 60_000 + 29_000) is AccessDecision.Allowed)
        // …después ya no.
        assertEquals(DenialReason.EXPIRED, reason(decide(qr, at = now + 60_000 + 31_000)))
    }

    @Test fun futureToken_withinSkew_ok_beyondSkew_denied() {
        val qr = QrCrypto.accessQr(signer, "2024001", 60, now + 20_000)
        assertTrue(decide(qr) is AccessDecision.Allowed)
        val far = QrCrypto.accessQr(signer, "2024001", 60, now + 45_000)
        assertEquals(DenialReason.NOT_YET_VALID, reason(decide(far)))
    }

    @Test fun tamperedToken_failsSignature() {
        val qr = QrCrypto.accessQr(signer, "2024001", 60, now)
        val parts = qr.split(".").toMutableList()
        parts[3] = (parts[3].toLong() + 3600).toString() // alarga la vigencia
        assertEquals(DenialReason.BAD_SIGNATURE, reason(decide(parts.joinToString("."))))
        val otherMat = qr.replace(QrCodec.b64("2024001"), QrCodec.b64("2024002"))
        assertEquals(DenialReason.BAD_SIGNATURE, reason(decide(otherMat)))
    }

    @Test fun wrongKey_failsSignature() {
        val other = SoftwareQrSigner.generate()
        val d = decide(QrCrypto.accessQr(other, "2024001", 60, now))
        assertEquals(DenialReason.BAD_SIGNATURE, reason(d))
        assertFalse(d.consumesNonce)
    }

    @Test fun reusedNonce_isDenied() {
        assertEquals(DenialReason.REUSED, reason(decide(QrCrypto.accessQr(signer, "2024001", 60, now), used = true)))
    }

    @Test fun lifetimeAboveGuardMax_isDenied() {
        val qr = QrCrypto.accessQr(signer, "2024001", 300, now)
        assertTrue(decide(qr) is AccessDecision.Allowed)
        assertEquals(DenialReason.LIFETIME_TOO_LONG, reason(decide(qr, p = GuardPolicy(maxLifetimeSec = 120))))
    }

    @Test fun unknownStudent_andMissingKey() {
        val qr = QrCrypto.accessQr(signer, "2024001", 60, now)
        assertEquals(DenialReason.NOT_REGISTERED, reason(decide(qr, student = null)))
        assertEquals(DenialReason.NO_KEY, reason(decide(qr, student = student(key = null))))
    }

    @Test fun statusReasons_consumeNonce() {
        val qr = QrCrypto.accessQr(signer, "2024001", 60, now)
        mapOf(
            StudentStatus.BAJA to DenialReason.BAJA,
            StudentStatus.SUSPENDIDO to DenialReason.SUSPENDIDO,
            StudentStatus.PENDING to DenialReason.PENDING,
            StudentStatus.REJECTED to DenialReason.REJECTED
        ).forEach { (st, r) ->
            val d = decide(qr, student = student(st))
            assertEquals(r, reason(d))
            assertTrue(d.consumesNonce)
        }
        assertTrue(decide(qr, student = student(StudentStatus.ACTIVO)) is AccessDecision.Allowed)
    }

    @Test fun outOfHours_isDenied() {
        assertEquals(DenialReason.OUT_OF_HOURS, reason(decide(QrCrypto.accessQr(signer, "2024001", 60, now), hours = false)))
    }

    @Test fun garbage_isInvalid() {
        listOf("", "hola", "UTCJA1.x.y", "UTCJA1.@@.1.2.abcdefgh.sig", "UTCJA1.QQ.1.x.abcdefghij.c2ln", "UTCJR1.a")
            .forEach { assertTrue(it, QrCodec.parse(it) is QrParseResult.Invalid) }
    }
}
