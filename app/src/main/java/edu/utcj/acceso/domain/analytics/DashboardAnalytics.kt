package edu.utcj.acceso.domain.analytics

import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.util.TimeUtil

/** Conteo de un día para la tendencia de 7 días. */
data class DayCount(val dayStartMs: Long, val allowed: Int, val denied: Int) {
    val total: Int get() = allowed + denied
}

data class DashboardStats(
    /** Accesos concedidos hoy (QR o manual; incluye métodos históricos). */
    val entriesToday: Int = 0,
    val attemptsToday: Int = 0,
    val deniedToday: Int = 0,
    /** Porcentaje 0–100 de intentos exitosos hoy. */
    val successRate: Float = 0f,
    val avgVerifyMs: Long = 0,
    val entriesYesterday: Int = 0,
    /** 24 posiciones: intentos por hora del día (hoy). */
    val byHour: List<Int> = List(24) { 0 },
    /** 7 posiciones, de hace 6 días a hoy. */
    val last7Days: List<DayCount> = emptyList(),
    val recent: List<AccessEvent> = emptyList()
) {
    /** Variación porcentual de entradas vs ayer; null si ayer no hubo datos. */
    val deltaVsYesterdayPct: Int?
        get() = if (entriesYesterday == 0) null
        else (((entriesToday - entriesYesterday) * 100f) / entriesYesterday).toInt()

    val peakHour: Int?
        get() = byHour.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
}

/** Cálculos puros del tablero (sin Android) para poder probarlos en la JVM. */
object DashboardCalculator {

    fun isSuccess(result: AccessResult) =
        result == AccessResult.ALLOWED || result == AccessResult.MANUAL || result == AccessResult.QR

    /**
     * @param events eventos de al menos los últimos 7 días (cualquier orden).
     * @param nowMs instante de referencia (hoy).
     */
    fun compute(events: List<AccessEvent>, nowMs: Long, recentLimit: Int = 8): DashboardStats {
        val todayStart = TimeUtil.startOfDayMs(nowMs)
        val yesterdayStart = TimeUtil.startOfDayMs(todayStart - 1)
        val today = events.filter { it.datetimeMs >= todayStart }
        val yesterday = events.filter { it.datetimeMs in yesterdayStart until todayStart }

        val ok = today.count { isSuccess(it.result) }
        val denied = today.count { it.result == AccessResult.DENIED }
        val durations = today.mapNotNull { it.verifyDurationMs }
        val hours = IntArray(24)
        today.forEach { hours[TimeUtil.hourOfDay(it.datetimeMs)]++ }

        // Inicio de cada uno de los últimos 7 días (respeta cambios de horario).
        val dayStarts = ArrayList<Long>(7)
        var cursor = todayStart
        repeat(7) {
            dayStarts.add(0, cursor)
            cursor = TimeUtil.startOfDayMs(cursor - 1)
        }
        val days = dayStarts.mapIndexed { i, start ->
            val end = if (i == dayStarts.lastIndex) Long.MAX_VALUE else dayStarts[i + 1]
            val inDay = events.filter { it.datetimeMs in start until end }
            DayCount(start, inDay.count { isSuccess(it.result) }, inDay.count { it.result == AccessResult.DENIED })
        }

        return DashboardStats(
            entriesToday = ok,
            attemptsToday = today.size,
            deniedToday = denied,
            successRate = if (today.isEmpty()) 0f else 100f * ok / today.size,
            avgVerifyMs = if (durations.isEmpty()) 0 else durations.average().toLong(),
            entriesYesterday = yesterday.count { isSuccess(it.result) },
            byHour = hours.toList(),
            last7Days = days,
            recent = events.sortedByDescending { it.datetimeMs }.take(recentLimit)
        )
    }
}

enum class AlertType { CONSECUTIVE_FAILURES, OUT_OF_HOURS }

data class AccessAlert(
    val type: AlertType,
    val title: String,
    val message: String,
    val matricula: String?,
    val timeMs: Long
)

/** Reglas de alertas del panel: 3+ fallos consecutivos y accesos fuera de horario. */
object AlertsEngine {
    const val CONSECUTIVE_FAILURE_THRESHOLD = 3

    fun build(eventsToday: List<AccessEvent>, startHour: Int, endHour: Int): List<AccessAlert> {
        val out = mutableListOf<AccessAlert>()
        eventsToday.groupBy { it.matricula }.forEach { (mat, list) ->
            val sorted = list.sortedByDescending { it.datetimeMs }
            val streak = sorted.takeWhile { it.result == AccessResult.DENIED }.size
            if (streak >= CONSECUTIVE_FAILURE_THRESHOLD) {
                val unknown = mat == "—" || mat.isBlank()
                out += AccessAlert(
                    type = AlertType.CONSECUTIVE_FAILURES,
                    title = if (unknown) "$streak QR no reconocidos seguidos" else "$streak fallos seguidos",
                    message = if (unknown) "Revisa el kiosco: varias personas no registradas intentaron entrar."
                    else "${sorted.first().nombre} ($mat) acumula $streak intentos denegados.",
                    matricula = mat.takeUnless { unknown },
                    timeMs = sorted.first().datetimeMs
                )
            }
        }
        eventsToday
            .filter { it.result == AccessResult.ALLOWED && !TimeUtil.isWithinHours(it.datetimeMs, startHour, endHour) }
            .forEach {
                out += AccessAlert(
                    type = AlertType.OUT_OF_HOURS,
                    title = "Acceso fuera de horario",
                    message = "${it.nombre} (${it.matricula}) a las ${TimeUtil.formatTime(it.datetimeMs)} · horario ${startHour}:00–${endHour}:00",
                    matricula = it.matricula,
                    timeMs = it.datetimeMs
                )
            }
        return out.distinctBy { Triple(it.type, it.matricula, it.timeMs) }.sortedByDescending { it.timeMs }
    }
}
