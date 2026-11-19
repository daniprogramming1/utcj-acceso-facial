package edu.utcj.acceso.util

/**
 * Nombre corto para pantallas públicas (kiosco): primer nombre + inicial de la siguiente palabra.
 * «Ana Sofía López» → «Ana S.», «Luis Ramírez Torres» → «Luis R.», «Carlos» → «Carlos».
 */
fun shortName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0]
        else -> "${parts[0]} ${parts[1].first().uppercaseChar()}."
    }
}

/** Iniciales para avatares sin foto (la app nunca guarda fotografías). */
fun initialsOf(name: String): String {
    val parts = name.trim()
        .split(Regex("\\s+"))
        .filter { p -> p.isNotEmpty() && p.first().isLetterOrDigit() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
