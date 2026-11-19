package edu.utcj.acceso.ui

import edu.utcj.acceso.analytics.TestTime.at
import edu.utcj.acceso.analytics.TestTime.event
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.ui.admin.groupConsecutiveDenials
import org.junit.Assert.assertEquals
import org.junit.Test

class LogGroupingTest {

    private fun denied(min: Int, mat: String = "A100", reason: String = "QR vencido") =
        event(at(2026, 10, 8, 13, min), AccessResult.DENIED, matricula = mat).copy(reason = reason)

    private fun allowed(min: Int, mat: String = "A100") = event(at(2026, 10, 8, 13, min), AccessResult.ALLOWED, matricula = mat)

    @Test
    fun consecutiveDenialsOfSameStudentAndReasonCollapse() {
        // Orden descendente, como la bitácora.
        val g = groupConsecutiveDenials(listOf(denied(16), denied(15), denied(14)))
        assertEquals(1, g.size)
        assertEquals(3, g[0].count)
        assertEquals(at(2026, 10, 8, 13, 16), g[0].latest.datetimeMs)
        assertEquals(at(2026, 10, 8, 13, 14), g[0].earliestMs)
    }

    @Test
    fun differentStudentReasonOrAnAllowedEntryBreaksTheRun() {
        val g = groupConsecutiveDenials(
            listOf(
                denied(20), denied(19),
                denied(18, mat = "B200"),
                denied(17, reason = "QR ya usado"),
                allowed(16),
                denied(15), denied(14)
            )
        )
        assertEquals(listOf(2, 1, 1, 1, 2), g.map { it.count })
    }

    @Test
    fun allowedEntriesNeverCollapse() {
        val g = groupConsecutiveDenials(listOf(allowed(3), allowed(2), allowed(1)))
        assertEquals(3, g.size)
        assertEquals(listOf(1, 1, 1), g.map { it.count })
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals(0, groupConsecutiveDenials(emptyList()).size)
    }
}
