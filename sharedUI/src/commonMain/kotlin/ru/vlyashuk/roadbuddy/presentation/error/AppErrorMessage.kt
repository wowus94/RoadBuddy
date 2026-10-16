package ru.vlyashuk.roadbuddy.presentation.error

import ru.vlyashuk.roadbuddy.domain.error.AppError

fun AppError.toMessage(): String = when (this) {
    AppError.Network -> "No internet connection"
    AppError.Unauthorized -> "Please sign in"
    AppError.Forbidden -> "You don't have permission to perform this action"
    AppError.NotFound -> "Request not found"
    AppError.InvalidCredentials -> "Incorrect email or password"
    AppError.EmailAlreadyInUse -> "An account with this email already exists"
    AppError.InvalidEmail -> "Enter a valid email address"
    AppError.WeakPassword -> "Password is too weak"
    AppError.Validation -> "Check the entered data"
    AppError.Conflict -> "The data has already been changed. Refresh and try again"
    AppError.Unknown -> "Something went wrong. Please try again"
}
