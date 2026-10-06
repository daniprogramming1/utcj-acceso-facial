package edu.utcj.acceso.domain.model

data class ConsentRecord(
    val matricula: String,
    val version: String,
    val acceptedAtMs: Long,
    val accepted: Boolean
) {
    companion object {
        const val CURRENT_VERSION = "1.0.0"
    }
}
