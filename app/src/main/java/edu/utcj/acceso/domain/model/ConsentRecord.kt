package edu.utcj.acceso.domain.model

data class ConsentRecord(
    val matricula: String,
    val version: String,
    val acceptedAtMs: Long,
    val accepted: Boolean
) {
    companion object {
        /** 2.0.0: aviso sin biometría (acceso con QR). */
        const val CURRENT_VERSION = "2.0.0"
    }
}
