package edu.utcj.acceso.analytics

import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.util.TimeUtil
import java.util.Calendar

/** Utilidades de prueba: instantes en la zona horaria del plantel. */
object TestTime {
    fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(TimeUtil.zone).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    /** Jueves 8 de octubre de 2026, 12:00 (America/Ciudad_Juarez). */
    val NOW = at(2026, 10, 8, 12)

    fun event(
        ms: Long,
        result: AccessResult,
        matricula: String = "A100",
        nombre: String = "Ana López",
        durationMs: Long? = null,
        method: AccessMethod = AccessMethod.FACE
    ) = AccessEvent(
        id = ms, datetimeMs = ms, matricula = matricula, nombre = nombre,
        result = result, method = method, verifyDurationMs = durationMs
    )
}
