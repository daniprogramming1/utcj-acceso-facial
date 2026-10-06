package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.security.AuthOutcome
import edu.utcj.acceso.data.security.GuardAuthManager
import edu.utcj.acceso.domain.model.GuardSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val guardAuth: GuardAuthManager
) {
    private val _session = MutableStateFlow<GuardSession?>(null)
    val session: StateFlow<GuardSession?> = _session.asStateFlow()

    fun isPasswordSet(): Boolean = guardAuth.isPasswordSet()
    fun isLockedOut(): Boolean = guardAuth.isLockedOut()
    fun lockRemainingMs(): Long = guardAuth.lockRemainingMs()

    fun setupPassword(password: CharArray, name: String = "Guardia UTCJ"): AuthOutcome {
        val result = guardAuth.setPassword(password, name)
        if (result is AuthOutcome.Success) {
            _session.value = GuardSession(GuardAuthManager.DEFAULT_GUARD_ID, result.displayName)
        }
        return result
    }

    fun login(password: CharArray): AuthOutcome {
        val result = guardAuth.verifyPassword(password)
        if (result is AuthOutcome.Success) {
            _session.value = GuardSession(GuardAuthManager.DEFAULT_GUARD_ID, result.displayName)
        }
        return result
    }

    fun changePassword(current: CharArray, newPassword: CharArray): AuthOutcome =
        guardAuth.changePassword(current, newPassword)

    fun requireReauth(password: CharArray): AuthOutcome = guardAuth.verifyPassword(password)

    fun logout() {
        _session.value = null
    }

    fun currentGuardName(): String =
        _session.value?.displayName ?: guardAuth.getDisplayName()
}
