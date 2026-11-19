package edu.utcj.acceso.domain.validation

/** Reglas del formulario de registro del alumno (mensajes en español para la UI). */
object RegistrationValidator {
    const val MATRICULA_MIN = 4
    const val MATRICULA_MAX = 15

    /** Mensaje que no se muestra mientras el alumno sigue escribiendo (validación en vivo). */
    const val MATRICULA_TOO_SHORT = "La matrícula es demasiado corta"

    /** Separador entre carrera y grupo en el dato guardado («Ingeniería en Software · 5A»). */
    const val CARRERA_SEPARATOR = " · "

    private val GRUPO = Regex("^[0-9]{1,2}[A-Z]{1,2}$")

    /** Normaliza: sin espacios y en mayúsculas. */
    fun normalizeMatricula(raw: String): String = raw.trim().replace(" ", "").uppercase()

    fun matriculaError(raw: String): String? {
        val m = normalizeMatricula(raw)
        return when {
            m.isEmpty() -> "Ingresa tu matrícula"
            !m.all { it.isLetterOrDigit() } -> "Usa solo letras y números"
            m.length < MATRICULA_MIN -> MATRICULA_TOO_SHORT
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

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** El correo es opcional; si se escribe, debe tener formato válido. */
    fun correoError(raw: String): String? {
        val c = raw.trim()
        return if (c.isEmpty() || EMAIL.matches(c)) null else "Revisa el formato del correo"
    }

    /** «5 a» → «5A». */
    fun normalizeGrupo(raw: String): String = raw.trim().replace(" ", "").uppercase()

    /** El grupo es opcional; si se escribe: 1–2 dígitos + 1–2 letras («5A», «10BT»). */
    fun grupoError(raw: String): String? {
        val g = normalizeGrupo(raw)
        return if (g.isEmpty() || GRUPO.matches(g)) null else "Usa número y letra, por ejemplo 5A"
    }

    /** Carrera + grupo como se guardan y se muestran: «Ingeniería en Software · 5A». */
    fun joinCarrera(carrera: String, grupo: String): String =
        listOf(carrera.trim(), normalizeGrupo(grupo)).filter { it.isNotEmpty() }.joinToString(CARRERA_SEPARATOR)

    /** Inverso de [joinCarrera] para precargar «Actualizar mis datos». Sin grupo válido → todo es carrera. */
    fun splitCarrera(stored: String): Pair<String, String> {
        val i = stored.lastIndexOf(CARRERA_SEPARATOR)
        if (i < 0) return stored.trim() to ""
        val tail = normalizeGrupo(stored.substring(i + CARRERA_SEPARATOR.length))
        return if (GRUPO.matches(tail)) stored.substring(0, i).trim() to tail else stored.trim() to ""
    }

    fun isValid(matricula: String, nombre: String): Boolean =
        matriculaError(matricula) == null && nombreError(nombre) == null
}
