package edu.utcj.acceso.ui.scan

import edu.utcj.acceso.data.qr.ScanOutcome
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.DenialReason

/** Lo que se muestra en la tarjeta del alumno tras escanear un QR de acceso. */
data class ScanCardModel(
    val allowed: Boolean,
    val nombre: String,
    val matricula: String,
    val carrera: String = "",
    val status: StudentStatus? = null,
    val reasonTitle: String? = null,
    val reasonDetail: String? = null,
    val timeMs: Long = System.currentTimeMillis()
) {
    /** Hay un alumno identificado (no «QR no válido»). */
    val known: Boolean get() = matricula.isNotBlank() && matricula != "—"

    companion object {
        fun denied(reason: DenialReason, atMs: Long, matricula: String = "—", nombre: String = reason.labelEs) =
            ScanCardModel(false, nombre, matricula, reasonTitle = reason.labelEs, reasonDetail = reason.detailEs, timeMs = atMs)
    }
}

fun ScanOutcome.Access.toCard(): ScanCardModel {
    val s = student
    return ScanCardModel(
        allowed = allowed,
        nombre = s?.nombre ?: "Alumno no registrado",
        matricula = s?.matricula ?: token.matricula,
        carrera = s?.carrera.orEmpty(),
        status = s?.status,
        reasonTitle = reason?.labelEs,
        reasonDetail = reason?.detailEs,
        timeMs = atMs
    )
}
