package ru.vlyashuk.roadbuddy.data.error

import kotlinx.coroutines.CancellationException
import ru.vlyashuk.roadbuddy.domain.error.AppError
import ru.vlyashuk.roadbuddy.domain.error.AppResult

internal class UnauthorizedException : Exception()
internal class ForbiddenException : Exception()

internal fun Throwable.toAppError(): AppError {
    val normalizedMessage = message.orEmpty().lowercase()
    return when {
        this is UnauthorizedException -> AppError.Unauthorized
        this is ForbiddenException -> AppError.Forbidden
        normalizedMessage.contains("network") ||
            normalizedMessage.contains("offline") ||
            normalizedMessage.contains("unavailable") ||
            normalizedMessage.contains("timeout") -> AppError.Network
        normalizedMessage.contains("unauthenticated") ||
            normalizedMessage.contains("user-not-found") -> AppError.Unauthorized
        normalizedMessage.contains("permission-denied") ||
            normalizedMessage.contains("permission denied") -> AppError.Forbidden
        normalizedMessage.contains("not found") -> AppError.NotFound
        normalizedMessage.contains("wrong-password") ||
            normalizedMessage.contains("invalid-credential") ||
            normalizedMessage.contains("invalid credential") -> AppError.InvalidCredentials
        normalizedMessage.contains("email-already-in-use") -> AppError.EmailAlreadyInUse
        normalizedMessage.contains("invalid-email") -> AppError.InvalidEmail
        normalizedMessage.contains("weak-password") -> AppError.WeakPassword
        normalizedMessage.contains("already exists") ||
            normalizedMessage.contains("conflict") -> AppError.Conflict
        else -> AppError.Unknown
    }
}

internal suspend inline fun <T> safeCall(
    crossinline block: suspend () -> T
): AppResult<T> = try {
    AppResult.Success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Throwable) {
    AppResult.Failure(exception.toAppError())
}
