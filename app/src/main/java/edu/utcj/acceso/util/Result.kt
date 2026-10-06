package edu.utcj.acceso.util

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
}

inline fun <T> runCatchingApp(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    AppResult.Error(e.message ?: "Error desconocido", e)
}
