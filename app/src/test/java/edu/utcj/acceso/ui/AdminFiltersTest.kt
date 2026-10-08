package edu.utcj.acceso.ui

import edu.utcj.acceso.analytics.TestTime.NOW
import edu.utcj.acceso.analytics.TestTime.at
import edu.utcj.acceso.analytics.TestTime.event
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.ui.admin.LogRange
import edu.utcj.acceso.ui.admin.StudentFilter
import edu.utcj.acceso.ui.admin.filterEvents
import edu.utcj.acceso.ui.admin.filterStudents
import edu.utcj.acceso.ui.admin.groupByDay
import edu.utcj.acceso.ui.admin.normalizeSearch
import edu.utcj.acceso.ui.admin.thresholdExplanation
import edu.utcj.acceso.util.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminFiltersTest {
    private val students = listOf(
        Student("A1", "José Pérez", "Software", StudentStatus.APPROVED),
        Student("A2", "Ana López", "Mecatrónica", StudentStatus.PENDING),
        Student("A3", "Luis Ramírez", "Software", StudentStatus.BAJA),
        Student("A4", "Carla Ruiz", "Redes", StudentStatus.ACTIVO),
        Student("A5", "Beto Soto", "Redes", StudentStatus.REJECTED)
    )

    @Test
    fun normalizeRemovesAccentsAndCase() {
        assertEquals("jose perez", normalizeSearch("  JOSÉ Pérez "))
    }

    @Test
    fun filterByQueryIgnoresAccents() {
        assertEquals(listOf("A1"), filterStudents(students, "jose", StudentFilter.ALL).map { it.matricula })
        assertEquals(listOf("A3"), filterStudents(students, "ramirez", StudentFilter.ALL).map { it.matricula })
        assertEquals(2, filterStudents(students, "software", StudentFilter.ALL).size)
    }

    @Test
    fun filterByStatus() {
        assertEquals(setOf("A1", "A4"), filterStudents(students, "", StudentFilter.APPROVED).map { it.matricula }.toSet())
        assertEquals(listOf("A2"), filterStudents(students, "", StudentFilter.PENDING).map { it.matricula })
        assertEquals(listOf("A3"), filterStudents(students, "", StudentFilter.INACTIVE).map { it.matricula })
        assertEquals(listOf("A5"), filterStudents(students, "", StudentFilter.REJECTED).map { it.matricula })
    }

    @Test
    fun resultsAreSortedByName() {
        val names = filterStudents(students, "", StudentFilter.ALL).map { it.nombre }
        assertEquals(names.sortedBy { it.lowercase() }, names)
    }

    @Test
    fun eventTextFilter() {
        val events = listOf(
            event(at(2026, 10, 8, 9), AccessResult.ALLOWED, matricula = "A1", nombre = "José Pérez"),
            event(at(2026, 10, 8, 10), AccessResult.DENIED, matricula = "A2", nombre = "Ana López")
        )
        assertEquals(1, filterEvents(events, "perez").size)
        assertEquals(1, filterEvents(events, "a2").size)
        assertEquals(2, filterEvents(events, "").size)
    }

    @Test
    fun groupByDayUsesHoyAyerHeaders() {
        val events = listOf(
            event(at(2026, 10, 8, 9), AccessResult.ALLOWED),
            event(at(2026, 10, 7, 9), AccessResult.ALLOWED),
            event(at(2026, 10, 7, 8), AccessResult.DENIED),
            event(at(2026, 10, 1, 8), AccessResult.DENIED)
        )
        val groups = groupByDay(events, NOW)
        assertEquals(listOf("Hoy", "Ayer"), groups.take(2).map { it.first })
        assertEquals(2, groups[1].second.size)
        assertEquals(3, groups.size)
    }

    @Test
    fun logRangeBounds() {
        val (from, to) = LogRange.Today.bounds(NOW)
        assertEquals(TimeUtil.startOfDayMs(NOW), from)
        assertNull(to)
        assertEquals(at(2026, 10, 2, 0), LogRange.Week.bounds(NOW).first)
        assertEquals(null to null, LogRange.All.bounds(NOW))
        val custom = LogRange.Custom(at(2026, 10, 1, 12), at(2026, 10, 3, 12)).bounds(NOW)
        assertEquals(at(2026, 10, 1, 0), custom.first)
        assertEquals(at(2026, 10, 4, 0) - 1, custom.second)
    }

    @Test
    fun thresholdExplanationWarnsBelowDefault() {
        assertTrue(thresholdExplanation(0.55f).startsWith("Permisivo"))
        assertTrue(thresholdExplanation(0.60f).startsWith("Equilibrado"))
        assertTrue(thresholdExplanation(0.75f).startsWith("Estricto"))
        assertTrue(thresholdExplanation(0.9f).startsWith("Muy estricto"))
    }
}
