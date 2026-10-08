package edu.utcj.acceso.data.security

/**
 * Estimación orientativa de fortaleza de contraseña para la UI.
 * No reemplaza la regla obligatoria de [GuardAuthManager] (mínimo 6 caracteres).
 */
object PasswordStrength {
    enum class Level(val score: Int, val labelEs: String) {
        VERY_WEAK(0, "Muy débil"),
        WEAK(1, "Débil"),
        FAIR(2, "Aceptable"),
        STRONG(3, "Fuerte"),
        VERY_STRONG(4, "Muy fuerte")
    }

    fun evaluate(password: CharSequence): Level {
        if (password.length < 6) return Level.VERY_WEAK
        var points = 0
        if (password.length >= 8) points++
        if (password.length >= 12) points++
        val classes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }
        points += (classes - 1).coerceAtLeast(0)
        if (password.length < 8) points--
        if (password.toSet().size <= 2) points = 0
        return when {
            points <= 0 -> Level.WEAK
            points <= 2 -> Level.FAIR
            points <= 4 -> Level.STRONG
            else -> Level.VERY_STRONG
        }
    }
}
