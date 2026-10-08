package edu.utcj.acceso.util

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
