package edu.utcj.acceso.time

import edu.utcj.acceso.analytics.TestTime
import edu.utcj.acceso.domain.time.DayPhase
import edu.utcj.acceso.domain.time.horizonProgress
import edu.utcj.acceso.util.TimeUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class DayPhaseTest {

    @Test
    fun boundariesFollowJuarezDay() {
        assertEquals(DayPhase.NIGHT, DayPhase.ofHour(4))
        assertEquals(DayPhase.DAWN, DayPhase.ofHour(5))
        assertEquals(DayPhase.DAWN, DayPhase.ofHour(7))
        assertEquals(DayPhase.DAY, DayPhase.ofHour(8))
        assertEquals(DayPhase.DAY, DayPhase.ofHour(16))
        assertEquals(DayPhase.DUSK, DayPhase.ofHour(17))
        assertEquals(DayPhase.DUSK, DayPhase.ofHour(19))
        assertEquals(DayPhase.NIGHT, DayPhase.ofHour(20))
        assertEquals(DayPhase.NIGHT, DayPhase.ofHour(0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidHour() {
        DayPhase.ofHour(24)
    }

    @Test
    fun hourIsReadInInstitutionTimeZone() {
        // 7:42 en Ciudad Juárez es amanecer, sin importar la zona del dispositivo de pruebas.
        assertEquals(7, TimeUtil.hourOfDay(TestTime.at(2026, 10, 8, 7, 42)))
        assertEquals(DayPhase.DAWN, DayPhase.ofHour(TimeUtil.hourOfDay(TestTime.at(2026, 10, 8, 7, 42))))
    }

    @Test
    fun progressGoesFromZeroToOneAndIsClamped() {
        assertEquals(0f, horizonProgress(60, 60), 0.0001f)
        assertEquals(0.3f, horizonProgress(42, 60), 0.0001f)
        assertEquals(1f, horizonProgress(0, 60), 0.0001f)
        assertEquals(0f, horizonProgress(90, 60), 0.0001f)
        assertEquals(1f, horizonProgress(-5, 60), 0.0001f)
        assertEquals(0f, horizonProgress(10, 0), 0.0001f)
    }
}
