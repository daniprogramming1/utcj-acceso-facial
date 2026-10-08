package edu.utcj.acceso.util

import edu.utcj.acceso.brand.BrandConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtil {
    val zone: TimeZone = TimeZone.getTimeZone(BrandConfig.TIME_ZONE_ID)
    val locale: Locale = Locale("es", "MX")

    private fun fmt(pattern: String) = SimpleDateFormat(pattern, locale).apply { timeZone = zone }

    fun formatDateTime(ms: Long): String = fmt("dd/MM/yyyy HH:mm:ss").format(Date(ms))

    fun formatDate(ms: Long): String = fmt("dd/MM/yyyy").format(Date(ms))

    fun formatTime(ms: Long): String = fmt("HH:mm").format(Date(ms))

    fun formatTimeSeconds(ms: Long): String = fmt("HH:mm:ss").format(Date(ms))

    /** «jueves, 8 de octubre» */
    fun formatLongDate(ms: Long): String = fmt("EEEE, d 'de' MMMM").format(Date(ms))

    /** «8 oct 2026» */
    fun formatShortDate(ms: Long): String = fmt("d MMM yyyy").format(Date(ms)).replace(".", "")

    /** «lun», «mar»… */
    fun weekdayShort(ms: Long): String = fmt("EEE").format(Date(ms)).replace(".", "").take(3)

    /** «Hoy», «Ayer» o «lunes 5 de octubre» para encabezados de lista. */
    fun dayHeader(ms: Long, nowMs: Long = System.currentTimeMillis()): String {
        val today = startOfDayMs(nowMs)
        val yesterday = startOfDayMs(today - 1)
        return when {
            ms >= today -> "Hoy"
            ms >= yesterday -> "Ayer"
            else -> fmt("EEEE d 'de' MMMM").format(Date(ms)).replaceFirstChar { it.uppercase() }
        }
    }

    fun startOfDayMs(ms: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(zone, locale)
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDayMs(ms: Long): Long {
        val cal = Calendar.getInstance(zone, locale)
        cal.timeInMillis = startOfDayMs(ms)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis - 1
    }

    /** Inicio del día de hace [days] días (0 = hoy). */
    fun daysAgoStartMs(days: Int, nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(zone, locale)
        cal.timeInMillis = startOfDayMs(nowMs)
        cal.add(Calendar.DAY_OF_MONTH, -days)
        return cal.timeInMillis
    }

    fun hourOfDay(ms: Long): Int {
        val cal = Calendar.getInstance(zone, locale)
        cal.timeInMillis = ms
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    fun isWithinHours(ms: Long, startHour: Int, endHour: Int): Boolean {
        val h = hourOfDay(ms)
        return if (startHour <= endHour) h in startHour until endHour else h >= startHour || h < endHour
    }

    /** «hace 5 min», «hace 2 h», o la hora si es de otro día. */
    fun relative(ms: Long, nowMs: Long = System.currentTimeMillis()): String {
        val diff = (nowMs - ms).coerceAtLeast(0)
        val min = diff / 60_000
        return when {
            min < 1 -> "Justo ahora"
            min < 60 -> "Hace $min min"
            min < 24 * 60 && ms >= startOfDayMs(nowMs) -> "Hace ${min / 60} h"
            else -> "${formatShortDate(ms)} ${formatTime(ms)}"
        }
    }

    /** Duración legible: «45 min», «2 h 10 min». */
    fun durationShort(ms: Long): String {
        val totalMin = (ms / 60_000).coerceAtLeast(0)
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "$h h ${m} min" else "$m min"
    }

    fun nowMs(): Long = System.currentTimeMillis()
}
