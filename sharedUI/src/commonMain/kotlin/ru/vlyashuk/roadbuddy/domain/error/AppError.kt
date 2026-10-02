package ru.vlyashuk.roadbuddy.domain.error

sealed interface AppError {

    data object Network : AppError

    data object Unauthorized : AppError

    data object Forbidden : AppError

    data object NotFound : AppError

    data object InvalidCredentials : AppError

    data object EmailAlreadyInUse : AppError

    data object InvalidEmail : AppError

    data object WeakPassword : AppError

    data object Validation : AppError

    data object Conflict : AppError

    data object Unknown : AppError
}