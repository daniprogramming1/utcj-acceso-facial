package edu.utcj.acceso.domain.model

data class Visitor(
    val id: Long = 0,
    val nombre: String,
    val motivo: String,
    val visitaA: String,
    val entradaMs: Long = System.currentTimeMillis(),
    val salidaMs: Long? = null,
    val registeredByGuard: String
) {
    val isInside: Boolean get() = salidaMs == null
}
