package edu.utcj.acceso.ui.kiosk

import edu.utcj.acceso.ui.scan.ScanCardModel
import edu.utcj.acceso.util.TimeUtil
import edu.utcj.acceso.util.initialsOf
import edu.utcj.acceso.util.shortName

/**
 * Lo que se puede mostrar en la pantalla del kiosco, que ve toda la fila (DISEÑO.md, Fase 2):
 * - Permitido: nombre corto («¡Pásale, Ana S.!»), sin matrícula ni carrera.
 * - Denegado por el código (vencido, ya usado…): el motivo, porque el alumno lo puede resolver.
 * - Denegado por la persona (baja, suspendido, pendiente…): mensaje genérico; el motivo queda
 *   solo en la bitácora y en el teléfono del guardia.
 */
data class KioskDisplay(
    val allowed: Boolean,
    val title: String,
    val subtitle: String?,
    val detail: String?,
    /** Iniciales del avatar, solo si se muestra a la persona. */
    val initials: String?
) {
    /** Texto que lee TalkBack al aparecer el resultado. */
    val spoken: String get() = listOfNotNull(title, subtitle, detail).joinToString(". ")
}

const val KIOSK_PRIVATE_TITLE = "Ahorita no"
const val KIOSK_PRIVATE_SUBTITLE = "Acércate con el guardia."
const val KIOSK_PRIVATE_DETAIL = "Él te dice qué pasó y cómo resolverlo."

fun kioskDisplay(card: ScanCardModel): KioskDisplay {
    if (card.allowed) {
        val name = shortName(card.nombre).takeIf { card.known && it.isNotBlank() }
        return KioskDisplay(
            allowed = true,
            title = if (name != null) "¡Pásale, $name!" else "¡Pásale!",
            subtitle = "Acceso permitido · ${TimeUtil.formatTime(card.timeMs)}",
            detail = null,
            initials = name?.let { initialsOf(card.nombre) }
        )
    }
    val reason = card.reason
    return if (reason != null && reason.kioskPublic) {
        KioskDisplay(
            allowed = false,
            title = "Acceso denegado",
            subtitle = reason.labelEs,
            detail = reason.detailEs,
            initials = null
        )
    } else {
        KioskDisplay(
            allowed = false,
            title = KIOSK_PRIVATE_TITLE,
            subtitle = KIOSK_PRIVATE_SUBTITLE,
            detail = KIOSK_PRIVATE_DETAIL,
            initials = null
        )
    }
}
