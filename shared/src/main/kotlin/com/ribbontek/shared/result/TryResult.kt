package com.ribbontek.shared.result

sealed class TryResult<out F, out S>

data class TryResultSuccess<out T>(val value: T) : TryResult<Nothing, T>()

data class TryResultFailure<out T>(val exception: T) : TryResult<T, Nothing>()

fun <R> tryRun(f: () -> R): TryResult<Throwable, R> =
    try {
        TryResultSuccess(f())
    } catch (e: Throwable) {
        TryResultFailure(e)
    }

fun <S> TryResultSuccess<S>.toResult(): S {
    return value
}

fun <T, R> TryResultSuccess<T>.toSuccess(action: (T) -> R): R = action(value)

fun <T, R> TryResultFailure<T>.toFailure(action: (T) -> R): R = action(exception)

fun <T, R> TryResult<T, R>.onSuccess(action: (R) -> Unit): TryResult<T, R> {
    if (this is TryResultSuccess) {
        action(value)
    }
    return this
}

fun <T, R> TryResult<T, R>.onFailure(action: (T) -> Unit): TryResult<T, R> {
    if (this is TryResultFailure) {
        action(exception)
    }
    return this
}
