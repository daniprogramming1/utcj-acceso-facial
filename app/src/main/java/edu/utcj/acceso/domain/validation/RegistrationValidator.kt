package edu.utcj.acceso.domain.validation

/** Reglas del formulario de registro del alumno (mensajes en español para la UI). */
object RegistrationValidator {
    const val MATRICULA_MIN = 4
    const val MATRICULA_MAX = 15

    /** Normaliza: sin espacios y en mayúsculas. */
    fun normalizeMatricula(raw: String): String = raw.trim().replace(" ", "").uppercase()

    fun matriculaError(raw: String): String? {
        val m = normalizeMatricula(raw)
        return when {
            m.isEmpty() -> "Ingresa tu matrícula"
            !m.all { it.isLetterOrDigit() } -> "Usa solo letras y números"
            m.length < MATRICULA_MIN -> "La matrícula es demasiado corta"
            m.length > MATRICULA_MAX -> "La matrícula es demasiado larga"
            else -> null
        }
    }

    fun nombreError(raw: String): String? {
        val n = raw.trim()
        return when {
            n.isEmpty() -> "Ingresa tu nombre completo"
            n.length < 3 -> "El nombre es demasiado corto"
            n.any { it.isDigit() } -> "El nombre no debe contener números"
            else -> null
        }
    }

    fun isValid(matricula: String, nombre: String): Boolean =
        matriculaError(matricula) == null && nombreError(nombre) == null
}
