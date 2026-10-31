package com.example.mse.data.error

import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 500,
    val factor: Double = 2.0,
    val maxDelayMs: Long = 8_000
) {
    fun delayBefore(nextAttempt: Int): Long =
        min(maxDelayMs, (initialDelayMs * factor.pow(nextAttempt - 2)).toLong())
}

data class RetryEvent(val nextAttempt: Int, val maxAttempts: Int, val delayMs: Long, val cause: Throwable)

suspend fun <T> retryWithBackoff(
    policy: RetryPolicy = RetryPolicy(),
    shouldRetry: (Throwable) -> Boolean = ::isTransientNetworkError,
    delayOverride: (Throwable) -> Long? = { null },
    onRetry: suspend (RetryEvent) -> Unit = {},
    block: suspend (attempt: Int) -> T
): T {
    var attempt = 1
    while (true) {
        try {
            return block(attempt)
        // Скасування корутини (закриття екрана) має одразу зупиняти й повтори.
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (attempt >= policy.maxAttempts || !shouldRetry(e)) throw e
            val wait = delayOverride(e) ?: policy.delayBefore(attempt + 1)
            onRetry(RetryEvent(attempt + 1, policy.maxAttempts, wait, e))
            delay(wait)
            attempt++
        }
    }
}

fun isTransientNetworkError(error: Throwable): Boolean = when (error) {
    // Наслідують IOException, але це помилка формату: повтор поверне той самий некоректний JSON.
    is MalformedJsonException, is EOFException -> false
    is HttpException -> error.code() == 408 || error.code() == 429 || error.code() in 500..599
    is IOException -> true
    else -> false
}
