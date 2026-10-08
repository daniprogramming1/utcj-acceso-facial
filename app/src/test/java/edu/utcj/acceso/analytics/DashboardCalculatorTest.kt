package edu.utcj.acceso.analytics

import edu.utcj.acceso.analytics.TestTime.NOW
import edu.utcj.acceso.analytics.TestTime.at
import edu.utcj.acceso.analytics.TestTime.event
import edu.utcj.acceso.domain.analytics.DashboardCalculator
import edu.utcj.acceso.domain.model.AccessResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardCalculatorTest {

    @Test
    fun emptyEvents_giveZeroedStats() {
        val s = DashboardCalculator.compute(emptyList(), NOW)
        assertEquals(0, s.entriesToday)
        assertEquals(0, s.attemptsToday)
        assertEquals(0f, s.successRate, 0.001f)
        assertEquals(0L, s.avgVerifyMs)
        assertEquals(24, s.byHour.size)
        assertEquals(7, s.last7Days.size)
        assertNull(s.peakHour)
        assertNull(s.deltaVsYesterdayPct)
    }

    @Test
    fun countsTodaySuccessAndDenied_ignoringYesterdayForToday() {
        val events = listOf(
            event(at(2026, 10, 8, 7, 10), AccessResult.ALLOWED, durationMs = 800),
            event(at(2026, 10, 8, 7, 40), AccessResult.QR),
            event(at(2026, 10, 8, 8, 5), AccessResult.MANUAL),
            event(at(2026, 10, 8, 8, 6), AccessResult.DENIED, durationMs = 1200),
            event(at(2026, 10, 7, 9, 0), AccessResult.ALLOWED),
            event(at(2026, 10, 7, 9, 5), AccessResult.ALLOWED)
        )
        val s = DashboardCalculator.compute(events, NOW)
        assertEquals(3, s.entriesToday)
        assertEquals(4, s.attemptsToday)
        assertEquals(1, s.deniedToday)
        assertEquals(75f, s.successRate, 0.001f)
        assertEquals(1000L, s.avgVerifyMs)
        assertEquals(2, s.entriesYesterday)
        assertEquals(50, s.deltaVsYesterdayPct)
        assertEquals(2, s.byHour[7])
        assertEquals(2, s.byHour[8])
        assertTrue(s.peakHour == 7 || s.peakHour == 8)
    }

    @Test
    fun last7Days_bucketsEachDayAndEndsToday() {
        val events = listOf(
            event(at(2026, 10, 2, 10), AccessResult.ALLOWED), // hace 6 días
            event(at(2026, 10, 1, 10), AccessResult.ALLOWED), // hace 7 días: fuera
            event(at(2026, 10, 5, 23, 59), AccessResult.DENIED),
            event(at(2026, 10, 8, 0, 1), AccessResult.ALLOWED)
        )
        val days = DashboardCalculator.compute(events, NOW).last7Days
        assertEquals(7, days.size)
        assertEquals(1, days.first().allowed)
        assertEquals(1, days[3].denied)
        assertEquals(1, days.last().allowed)
        assertEquals(3, days.sumOf { it.total })
    }

    @Test
    fun recent_isSortedNewestFirstAndLimited() {
        val events = (0 until 12).map { event(at(2026, 10, 8, 6, it * 2), AccessResult.ALLOWED) }
        val recent = DashboardCalculator.compute(events.shuffled(), NOW, recentLimit = 5).recent
        assertEquals(5, recent.size)
        assertEquals(events.last().datetimeMs, recent.first().datetimeMs)
        assertTrue(recent.zipWithNext().all { (a, b) -> a.datetimeMs >= b.datetimeMs })
    }
}
