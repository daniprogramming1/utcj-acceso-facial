package edu.utcj.acceso.analytics

import edu.utcj.acceso.analytics.TestTime.at
import edu.utcj.acceso.analytics.TestTime.event
import edu.utcj.acceso.domain.analytics.AlertType
import edu.utcj.acceso.domain.analytics.AlertsEngine
import edu.utcj.acceso.domain.model.AccessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsEngineTest {

    @Test
    fun threeConsecutiveDenials_raiseAlert() {
        val events = listOf(
            event(at(2026, 10, 8, 9, 0), AccessResult.DENIED),
            event(at(2026, 10, 8, 9, 1), AccessResult.DENIED),
            event(at(2026, 10, 8, 9, 2), AccessResult.DENIED)
        )
        val alerts = AlertsEngine.build(events, 6, 22)
        assertEquals(1, alerts.size)
        assertEquals(AlertType.CONSECUTIVE_FAILURES, alerts[0].type)
        assertEquals("A100", alerts[0].matricula)
        assertTrue(alerts[0].title.contains("3"))
    }

    @Test
    fun successBreaksStreak() {
        val events = listOf(
            event(at(2026, 10, 8, 9, 0), AccessResult.DENIED),
            event(at(2026, 10, 8, 9, 1), AccessResult.DENIED),
            event(at(2026, 10, 8, 9, 2), AccessResult.ALLOWED),
            event(at(2026, 10, 8, 9, 3), AccessResult.DENIED)
        )
        assertTrue(AlertsEngine.build(events, 6, 22).isEmpty())
    }

    @Test
    fun unknownFaces_areGroupedWithoutMatricula() {
        val events = (0 until 4).map { event(at(2026, 10, 8, 10, it), AccessResult.DENIED, matricula = "—", nombre = "Desconocido") }
        val alert = AlertsEngine.build(events, 6, 22).single()
        assertNull(alert.matricula)
        assertTrue(alert.title.contains("no reconocidos"))
    }

    @Test
    fun allowedOutsideHours_raisesOutOfHoursAlert() {
        val events = listOf(
            event(at(2026, 10, 8, 5, 30), AccessResult.ALLOWED),
            event(at(2026, 10, 8, 12, 0), AccessResult.ALLOWED),
            event(at(2026, 10, 8, 4, 0), AccessResult.MANUAL) // manual: autorizado por guardia, sin alerta
        )
        val alerts = AlertsEngine.build(events, 6, 22)
        assertEquals(1, alerts.size)
        assertEquals(AlertType.OUT_OF_HOURS, alerts[0].type)
    }

    @Test
    fun alertsAreSortedNewestFirst() {
        val events = listOf(
            event(at(2026, 10, 8, 5, 0), AccessResult.ALLOWED, matricula = "B1"),
            event(at(2026, 10, 8, 23, 0), AccessResult.ALLOWED, matricula = "B2")
        )
        val alerts = AlertsEngine.build(events, 6, 22)
        assertEquals("B2", alerts.first().matricula)
    }
}
