package edu.utcj.acceso

/** Acceso a datos de compilación desde la UI (y valores estables en capturas de pantalla). */
object BuildConfigProxy {
    val VERSION_NAME: String get() = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int get() = BuildConfig.VERSION_CODE
}
