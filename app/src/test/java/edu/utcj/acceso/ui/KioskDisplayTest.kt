package edu.utcj.acceso.ui

import edu.utcj.acceso.analytics.TestTime
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.ui.kiosk.KIOSK_PRIVATE_SUBTITLE
import edu.utcj.acceso.ui.kiosk.KIOSK_PRIVATE_TITLE
import edu.utcj.acceso.ui.kiosk.kioskDisplay
import edu.utcj.acceso.ui.scan.ScanCardModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** El kiosco lo ve toda la fila: nunca debe mostrar matrícula ni motivos personales. */
class KioskDisplayTest {

    private val at = TestTime.at(2026, 10, 8, 7, 42)

    private fun denied(reason: DenialReason?) = ScanCardModel(
        allowed = false, nombre = "Luis Ramírez Torres", matricula = "20220311",
        carrera = "Redes", reasonTitle = reason?.labelEs, reasonDetail = reason?.detailEs,
        timeMs = at, reason = reason
    )

    @Test
    fun allowedShowsShortNameOnly() {
        val d = kioskDisplay(ScanCardModel(true, "Ana Sofía López", "20231045", "Ingeniería en Software", timeMs = at))
        assertTrue(d.allowed)
        assertEquals("¡Pásale, Ana S.!", d.title)
        assertEquals("Acceso permitido · 07:42", d.subtitle)
        assertEquals("AS", d.initials)
        assertFalse(d.spoken.contains("20231045"))
        assertFalse(d.spoken.contains("López"))
        assertFalse(d.spoken.contains("Software"))
    }

    @Test
    fun personalReasonsAreHidden() {
        listOf(
            DenialReason.BAJA, DenialReason.SUSPENDIDO, DenialReason.PENDING, DenialReason.REJECTED,
            DenialReason.NOT_REGISTERED, DenialReason.NO_KEY, DenialReason.BAD_SIGNATURE
        ).forEach { reason ->
            val d = kioskDisplay(denied(reason))
            assertEquals(reason.name, KIOSK_PRIVATE_TITLE, d.title)
            assertEquals(reason.name, KIOSK_PRIVATE_SUBTITLE, d.subtitle)
            assertNull(reason.name, d.initials)
            assertFalse(reason.name, d.spoken.contains(reason.labelEs))
            assertFalse(reason.name, d.spoken.contains("Luis"))
            assertFalse(reason.name, d.spoken.contains("20220311"))
        }
    }

    @Test
    fun codeReasonsAreShownBecauseTheStudentCanFixThem() {
        val d = kioskDisplay(denied(DenialReason.EXPIRED))
        assertEquals("Acceso denegado", d.title)
        assertEquals(DenialReason.EXPIRED.labelEs, d.subtitle)
        assertNull(d.initials)
        assertFalse(d.spoken.contains("Luis"))
    }

    @Test
    fun unknownReasonDefaultsToPrivate() {
        assertEquals(KIOSK_PRIVATE_TITLE, kioskDisplay(denied(null)).title)
    }
}
