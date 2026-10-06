package edu.utcj.acceso.domain.model

data class GuardSession(
    val guardId: String,
    val displayName: String,
    val loggedInAtMs: Long = System.currentTimeMillis(),
    val isAuthenticated: Boolean = true
)
