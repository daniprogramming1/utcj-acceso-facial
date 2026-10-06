package edu.utcj.acceso.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtil {
    private val zone: TimeZone = TimeZone.getTimeZone("America/Ciudad_Juarez")
    private val locale = Locale("es", "MX")

    fun formatDateTime(ms: Long): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale)
        fmt.timeZone = zone
        return fmt.format(Date(ms))
    }

    fun formatDate(ms: Long): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", locale)
        fmt.timeZone = zone
        return fmt.format(Date(ms))
    }

    fun formatTime(ms: Long): String {
        val fmt = SimpleDateFormat("HH:mm", locale)
        fmt.timeZone = zone
        return fmt.format(Date(ms))
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

    fun hourOfDay(ms: Long): Int {
        val cal = Calendar.getInstance(zone, locale)
        cal.timeInMillis = ms
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    fun isWithinHours(ms: Long, startHour: Int, endHour: Int): Boolean {
        val h = hourOfDay(ms)
        return if (startHour <= endHour) h in startHour until endHour else h >= startHour || h < endHour
    }

    fun nowMs(): Long = System.currentTimeMillis()
}
