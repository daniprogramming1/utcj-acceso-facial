package edu.utcj.acceso.domain.time

/**
 * Momento del día en Ciudad Juárez. Colorea la banda de horizonte del «Pase vivo»
 * (DISEÑO.md §2): una captura de pantalla vieja delata la hora en la que se tomó.
 */
enum class DayPhase(val label: String) {
    DAWN("Amanecer"),
    DAY("Día"),
    DUSK("Atardecer"),
    NIGHT("Noche");

    companion object {
        /** 5–7 amanecer · 8–16 día · 17–19 atardecer · 20–4 noche. */
        fun ofHour(hour: Int): DayPhase {
            require(hour in 0..23) { "Hora fuera de rango: $hour" }
            return when (hour) {
                in 5..7 -> DAWN
                in 8..16 -> DAY
                in 17..19 -> DUSK
                else -> NIGHT
            }
        }
    }
}

/**
 * Avance del sol sobre la sierra, de 0 (QR recién generado) a 1 (a punto de renovarse).
 * Valores fuera de rango se acotan; un total no positivo cuenta como recién generado.
 */
fun horizonProgress(secondsLeft: Int, totalSeconds: Int): Float {
    if (totalSeconds <= 0) return 0f
    return (1f - secondsLeft.toFloat() / totalSeconds).coerceIn(0f, 1f)
}
