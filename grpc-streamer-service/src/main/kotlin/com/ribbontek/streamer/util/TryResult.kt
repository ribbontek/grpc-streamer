package com.ribbontek.streamer.util

sealed class TryResult<out T>

class TryResultSuccess<out T>(val value: T) : TryResult<T>()

class TryResultFailure(val exception: Throwable) : TryResult<Nothing>()

fun <T> tryRun(block: () -> T): TryResult<T> {
    return try {
        TryResultSuccess(block())
    } catch (t: Throwable) {
        TryResultFailure(t)
    }
}

fun <T> TryResultSuccess<T>.toResult(): T {
    return value
}

fun <T, R> TryResultSuccess<T>.toSuccess(action: (T) -> R): R {
    return action(value)
}

fun <R> TryResultFailure.toFailure(action: (Throwable) -> R): R {
    return action(exception)
}

fun <T> TryResult<T>.onSuccess(action: (T) -> Unit): TryResult<T> {
    if (this is TryResultSuccess) {
        action(value)
    }
    return this
}

fun <T> TryResult<T>.onFailure(action: (Throwable) -> Unit): TryResult<T> {
    if (this is TryResultFailure) {
        action(exception)
    }
    return this
}
