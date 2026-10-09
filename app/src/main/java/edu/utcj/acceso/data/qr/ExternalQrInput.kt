package edu.utcj.acceso.data.qr

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entrada de códigos que no vienen de la cámara del escáner (lectores externos o pruebas
 * automatizadas). Solo la pantalla de escaneo visible los procesa.
 */
@Singleton
class ExternalQrInput @Inject constructor() {
    private val _codes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val codes: SharedFlow<String> = _codes.asSharedFlow()

    fun submit(raw: String) {
        _codes.tryEmit(raw)
    }
}
